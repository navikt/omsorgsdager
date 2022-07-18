package no.nav.omsorgsdager.saksnummer

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import org.json.JSONObject
import java.net.URI

internal class OmsorgspengerSakGateway(
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>,
    omsorgspengerSakUrl: URI
) : AzureAwareGateway(
    navn = "OmsorgspengerSakGateway",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUri = URI("$omsorgspengerSakUrl/isready")
) {

    private val omsorgspengerSaksnummerUrl = "$omsorgspengerSakUrl/saksnummer"

    internal suspend fun hentSaksnummer(
        identitetsnummer: Identitetsnummer,
        correlationId: CorrelationId
    ): OmsorgspengerSaksnummer? {

        val (httpStatusCode, responseBody) = omsorgspengerSaksnummerUrl.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(HttpHeaders.Accept, "application/json")
            it.header(HttpHeaders.XCorrelationId, correlationId)
            it.jsonBody("""{"identitetsnummer":"$identitetsnummer"}""")
        }.readTextOrThrow()

        return when (httpStatusCode) {
            HttpStatusCode.NotFound -> null
            HttpStatusCode.OK -> JSONObject(responseBody).getString("saksnummer").somOmsorgspengerSaksnummer()
            else -> throw IllegalStateException("HttpStatusCode=[${httpStatusCode.value}], Response=[$responseBody] fra omsorgspenger-sak.")
        }
    }
}