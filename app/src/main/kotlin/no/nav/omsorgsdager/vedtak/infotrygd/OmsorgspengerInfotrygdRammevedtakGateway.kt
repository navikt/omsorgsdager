package no.nav.omsorgsdager.vedtak.infotrygd

import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
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
    hentRammevedtakFraInfotrygdScopes: Set<String>,
    omsorgspengerInfotrygdRammevedtakBaseUrl: URI
) : AzureAwareGateway(
    navn = "OmsorgspengerInfotrygdRammevedtakGateway",
    accessTokenClient = accessTokenClient,
    scopes = hentRammevedtakFraInfotrygdScopes,
    pingUri = URI("$omsorgspengerInfotrygdRammevedtakBaseUrl/isready")) {

    private val rammevedtakUrl = "$omsorgspengerInfotrygdRammevedtakBaseUrl/rammevedtak"

    internal suspend fun hentInnvilgedeVedtak(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId
    ) : List<InfotrygdInnvilgetVedtak> {

        val (_, response, result) = rammevedtakUrl
            .httpPost()
            .header(HttpHeaders.Authorization, authorizationHeader())
            .header(HttpHeaders.Accept, "application/json")
            .header(HttpHeaders.ContentType, "application/json")
            .header(HttpHeaders.XCorrelationId, correlationId)
            .body(
                body = JSONObject().also { root ->
                    root.put("fom", periode.fom.toString())
                    root.put("tom", periode.tom.toString())
                    root.put("personIdent", identitetsnummer)
                }.toString(),
                charset = Charsets.UTF_8
            ).responseString(charset = Charsets.UTF_8)

        val json = result.fold(
            success = { JSONObject(it) },
            failure = {
                throw IllegalStateException("HTTP ${response.statusCode} - ${it.message}")
            }
        )

        val rammevedtak = json.getJSONObject("rammevedtak")

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

        return utvidetRett
            .asSequence()
            .plus(midlertidigAlene)
            .toList()
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
        private fun JSONObject.identitetsnummer(): Identitetsnummer = getString("id").somIdentitetsnummer()
        private fun JSONObject.fødselsdato(): LocalDate = LocalDate.parse(getString("fødselsdato"))
        private fun JSONArray.mapJSONObject() = map { it as JSONObject }
    }
}