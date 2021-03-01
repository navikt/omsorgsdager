package no.nav.omsorgsdager.saksnummer

import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import org.json.JSONObject
import java.net.URI

internal class OmsorgspengerSakGateway(
    accessTokenClient: AccessTokenClient,
    hentSaksnummerFraOmsorgspengerSakScopes: Set<String>,
    omsorgspengerSakUrl: URI
) : AzureAwareGateway(
    navn = "OmsorgspengerSakGateway",
    accessTokenClient = accessTokenClient,
    scopes = hentSaksnummerFraOmsorgspengerSakScopes,
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
            .body(
                body = JSONObject().also { root ->
                    root.put("identitetsnummer", identitetsnummer)
                }.toString(),
                charset = Charsets.UTF_8
            ).responseString(charset = Charsets.UTF_8)

        result.fold(
            success = {
                return JSONObject(it)
                .getString("saksnummer")
                .somOmsorgspengerSaksnummer() },
            failure = { return null }
        )

    }
}