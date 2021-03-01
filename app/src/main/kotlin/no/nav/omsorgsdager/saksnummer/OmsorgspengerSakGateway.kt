package no.nav.omsorgsdager.saksnummer

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import org.json.JSONObject
import java.net.URI

internal class OmsorgspengerSakGateway(
    private val accessTokenClient: AccessTokenClient,
    private val hentSaksnummerFraOmsorgspengerSakScopes: Set<String>,
    private val omsorgspengerSakUrl: URI
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val pingUrl = "$omsorgspengerSakUrl/isready"
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

    private fun authorizationHeader() =
        cachedAccessTokenClient.getAccessToken(hentSaksnummerFraOmsorgspengerSakScopes).asAuthoriationHeader()

    override suspend fun check() =
        Result.merge(
            "OmsorgspengerSakGateway",
            accessTokenCheck(),
            pingOmsorgspengerSakCheck()
        )

    private fun accessTokenCheck() = kotlin.runCatching {
        accessTokenClient.getAccessToken(hentSaksnummerFraOmsorgspengerSakScopes).let {
            (SignedJWT.parse(it.accessToken).jwtClaimsSet.getStringArrayClaim("roles")?.toList()
                ?: emptyList()).contains("access_as_application")
        }
    }.fold(
        onSuccess = {
            when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }
        },
        onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )


    private suspend fun pingOmsorgspengerSakCheck() =
        pingUrl.httpGet().awaitStringResponseResult().third.fold(
            success = { Healthy("PingOmsorgspengerSak", "OK: $it") },
            failure = { UnHealthy("PingOmsorgspengerSak", "Feil: ${it.message}") }
        )
}