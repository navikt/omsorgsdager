package no.nav.omsorgsdager.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgsdager.config.Environment
import no.nav.omsorgsdager.config.ServiceUser
import no.nav.omsorgsdager.config.hentRequiredEnv
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class PdlClient(
        env: Environment,
        accessTokenClient: AccessTokenClient,
        private val httpClient: HttpClient,
        private val objectMapper: ObjectMapper
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val pdlBaseUrl = env.hentRequiredEnv("PDL_BASE_URL")
    private val proxyScope = setOf(env.hentRequiredEnv("PROXY_SCOPES"))

    suspend fun getPersonInfo(ident: Set<String>): HentPdlBolkResponse {
        return httpClient.post<HttpStatement>("$pdlBaseUrl") {
            header(HttpHeaders.Authorization, getAuthorizationHeader())
            header("Nav-Consumer-Token", getAuthorizationHeader())
            header("Nav-Consumer-Id", "srvomsorgsdager")
            header("TEMA", "OMS")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = hentIdenterQuery(ident)
        }.receive<String>().also {
            secureLogger.info("PdlResponse=${JSONObject(it)}")
        }.let { objectMapper.readValue(it) }
    }

    private fun getAuthorizationHeader() = cachedAccessTokenClient.getAccessToken(proxyScope).asAuthoriationHeader()

    override suspend fun check() = kotlin.runCatching {
        httpClient.options<HttpStatement>(pdlBaseUrl) {
            header(HttpHeaders.Authorization, getAuthorizationHeader())
        }.execute().status
    }.fold(
            onSuccess = { statusCode ->
                when (HttpStatusCode.OK == statusCode) {
                    true -> Healthy("PdlClient", "OK")
                    false -> UnHealthy("PdlClient", "Feil: Mottok Http Status Code ${statusCode.value}")
                }
            },
            onFailure = {
                UnHealthy("PdlClient", "Feil: ${it.message}")
            }
    )

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }
}