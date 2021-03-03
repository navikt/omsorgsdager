package no.nav.omsorgsdager

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.nimbusds.jwt.SignedJWT
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import java.net.URI

internal abstract class AzureAwareGateway(
    private val navn: String,
    private val accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>,
    protected val pingUri: URI) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    protected fun authorizationHeader() =
        cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()

    override suspend fun check() =
        Result.merge(
            navn,
            accessTokenCheck(),
            pingCheck()
        )

    private fun accessTokenCheck() = kotlin.runCatching {
        accessTokenClient.getAccessToken(scopes).let {
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

    open suspend fun pingCheck() : Result = pingUri.toString().httpGet().awaitStringResponseResult().third.fold(
        success = { Healthy("PingCheck", "OK: $it") },
        failure = { UnHealthy("PingCheck", "Feil: ${it.message}") }
    )
}