package no.nav.omsorgsdager.vedtak.infotrygd

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.Kilde
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.LocalDate

internal class OmsorgspengerInfotrygdRammevedtakGateway(
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>,
    omsorgspengerInfotrygdRammevedtakBaseUrl: URI
) : AzureAwareGateway(
    navn = "OmsorgspengerInfotrygdRammevedtakGateway",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUri = URI("$omsorgspengerInfotrygdRammevedtakBaseUrl/isready")
) {

    private val rammevedtakUrl = "$omsorgspengerInfotrygdRammevedtakBaseUrl/rammevedtak"

    internal suspend fun hentInnvilgedeVedtak(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId
    ): List<InfotrygdInnvilgetVedtak> {

        val (httpStatusCode, responseBody) = rammevedtakUrl.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(HttpHeaders.Accept, "application/json")
            it.header(HttpHeaders.XCorrelationId, "$correlationId")
            it.jsonBody("""{"fom":"${periode.fom}", "tom":"${periode.tom}", "personIdent": "$identitetsnummer"}""")
        }.readTextOrThrow()


        return when (httpStatusCode) {
            HttpStatusCode.OK -> {
                val rammevedtak = JSONObject(responseBody).getJSONObject("rammevedtak")

                val utvidetRett = rammevedtak.getArray("UtvidetRett").mapJSONObject().map {
                    KroniskSyktBarnInfotrygdInnvilgetVedtak(
                        kilder = it.kilder(),
                        vedtatt = it.vedtatt(),
                        gyldigFraOgMed = it.periode().fom,
                        gyldigTilOgMed = it.periode().tom,
                        barnetsIdentitetsnummer = it.barn().identitetsnummer(),
                        barnetsFødselsdato = it.barn().fødselsdato()
                    )
                }

                val midlertidigAlene = rammevedtak.getArray("MidlertidigAleneOmOmsorgen").mapJSONObject().map {
                    MidlertidigAleneInfotrygdInnvilgetVedtak(
                        kilder = it.kilder(),
                        vedtatt = it.vedtatt(),
                        gyldigFraOgMed = it.periode().fom,
                        gyldigTilOgMed = it.periode().tom
                    )
                }

                val aleneOmsorg = rammevedtak.getArray("AleneOmOmsorgen").mapJSONObject().map {
                    AleneOmsorgInfotrygdInnvilgetVedtak(
                        kilder = it.kilder(),
                        vedtatt = it.vedtatt(),
                        gyldigFraOgMed = it.periode().fom,
                        gyldigTilOgMed = it.periode().tom,
                        barnetsIdentitetsnummer = it.barn().identitetsnummer(),
                        barnetsFødselsdato = it.barn().fødselsdato()
                    )
                }

                utvidetRett.plus(midlertidigAlene).plus(aleneOmsorg)

            }
            else -> throw IllegalStateException("HttpStatusCode=[${httpStatusCode.value}], Response=[${responseBody}] fra omsorgspenger-infotrygd-rammevedtak.")
        }
    }

    private companion object {
        private fun JSONObject.getArray(key: String) = when (has(key) && get(key) is JSONArray) {
            true -> getJSONArray(key)
            false -> JSONArray()
        }

        private fun JSONObject.periode() = Periode(
            fom = LocalDate.parse(getString("gyldigFraOgMed")),
            tom = LocalDate.parse(getString("gyldigTilOgMed"))
        )

        private fun JSONObject.kilder() = getJSONArray("kilder")
            .map { it as JSONObject }
            .map {
                Kilde(
                    id = it.getString("id"),
                    type = it.getString("type")
                )
            }.toSet()

        private fun JSONObject.barn() = getJSONObject("barn")
        private fun JSONObject.vedtatt(): LocalDate = LocalDate.parse(getString("vedtatt"))
        private fun JSONObject.identitetsnummer(): Identitetsnummer? = when (getString("type")) {
            "PersonIdent" -> getString("id").somIdentitetsnummer()
            else -> null
        }

        private fun JSONObject.fødselsdato(): LocalDate = LocalDate.parse(getString("fødselsdato"))
        private fun JSONArray.mapJSONObject() = map { it as JSONObject }
    }
}