package no.nav.omsorgsdager.vedtak.rammemeldinger

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.tidspunkt
import no.nav.omsorgsdager.vedtak.dto.AleneOmsorgInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.Kilde
import org.json.JSONObject
import java.net.URI

internal class RammemeldingerGateway(
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>,
    baseUrl: URI
) : AzureAwareGateway(
    navn = "RammemeldingerGateway",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUri = URI("$baseUrl/isready")) {

    private val hentAleneOmsorgUrl = "$baseUrl/alene-om-omsorgen?saksnummer="

    internal suspend fun hentAleneOmsorg(
        saksnummer: OmsorgspengerSaksnummer,
        correlationId: CorrelationId) : List<AleneOmsorgInnvilgetVedtak> {

        val urlMedSaksnummer = "${hentAleneOmsorgUrl}${saksnummer}"

        val (httpStatusCode, response) = urlMedSaksnummer.httpGet { builder ->
            builder.header(HttpHeaders.Authorization, authorizationHeader())
            builder.header(HttpHeaders.XCorrelationId, "$correlationId")
            builder.accept(ContentType.Application.Json)
        }.readTextOrThrow()

        require(httpStatusCode == HttpStatusCode.OK) {
            "HTTP ${httpStatusCode.value} fra $urlMedSaksnummer. Response=$response"
        }

        return JSONObject(response).getJSONArray("aleneOmOmsorgen").map { it as JSONObject }.map { aleneOm -> AleneOmsorgInnvilgetVedtak(
            tidspunkt = aleneOm.getString("registrert").tidspunkt(),
            periode = Periode(
                fom = aleneOm.getString("gjelderFraOgMed").dato(),
                tom = aleneOm.getString("gjelderTilOgMed").dato()
            ),
            barn = aleneOm.getJSONObject("barn").let { barn -> Barn(
                identitetsnummer = barn.getString("identitetsnummer").somIdentitetsnummer(),
                fødselsdato = barn.getString("fødselsdato").dato()
            )},
            kilder = aleneOm.getJSONObject("kilde").let { kilde -> setOf(Kilde(
                id = kilde.getString("id"),
                type = kilde.getString("type")
            ))}
        )}
    }
}