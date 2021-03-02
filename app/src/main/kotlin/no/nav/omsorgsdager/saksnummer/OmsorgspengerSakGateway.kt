package no.nav.omsorgsdager.saksnummer

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.getOrNull
import io.ktor.http.*
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
    pingUri = URI("$omsorgspengerSakUrl/isready")) {

    private val omsorgspengerSaksnummerUrl = "$omsorgspengerSakUrl/saksnummer"

    internal suspend fun hentSaksnummer(
        identitetsnummer: Identitetsnummer,
        correlationId: CorrelationId
    ): OmsorgspengerSaksnummer? {
        val (_, response, result) = omsorgspengerSaksnummerUrl
            .httpPost()
            .header(HttpHeaders.Authorization, authorizationHeader())
            .header(HttpHeaders.Accept, "application/json")
            .header(HttpHeaders.ContentType, "application/json")
            .header(HttpHeaders.XCorrelationId, correlationId)
            .body("""{"identitetsnummer":"$identitetsnummer"}""")
            .awaitStringResponseResult()

        return when (response.statusCode) {
            404 -> null
            200 -> JSONObject(result.get()).getString("saksnummer").somOmsorgspengerSaksnummer()
            else -> throw IllegalStateException("HttpStatusCode=[${response.statusCode}], Response=[${result.getOrNull()}] fra omsorgspenger-sak.")
        }
    }
}