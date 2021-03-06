package no.nav.omsorgsdager.tilgangsstyring

import io.ktor.client.features.ResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.toByteArray
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.omsorgsdager.CorrelationId
import org.slf4j.LoggerFactory
import java.net.URI

internal class OmsorgspengerTilgangsstyringGateway(
    baseUri: URI
): HealthCheck {
    private val tilgangUri = URI("$baseUri/api/tilgang/personer")
    private val pingUri = URI("$baseUri/isready")

    internal suspend fun harTilgang(
        token: Token,
        operasjon: Operasjon,
        correlationId: CorrelationId): Boolean {
        return tilgangUri.httpPost {
            it.header(HttpHeaders.Authorization, token.authorizationHeader)
            it.header(HttpHeaders.XCorrelationId, "$correlationId")
            it.jsonBody(operasjon.somJsonBody())
        }.second.håndterResponse()
    }

    private suspend fun Result<HttpResponse>.håndterResponse(): Boolean = fold(
        onSuccess = { response -> response.håndterResponse() },
        onFailure = { cause ->
            when (cause is ResponseException) {
                true -> { cause.response.håndterResponse() }
                else -> throw cause
            }
        }
    )

    private suspend fun HttpResponse.håndterResponse() = when (status) {
        HttpStatusCode.NoContent -> true
        HttpStatusCode.Forbidden -> false
        else -> {
            logger.error("HTTP ${status.value} fra omsorgspenger-tilgangsstyring, response: ${String(content.toByteArray())}")
            throw IllegalStateException("Uventet feil ved sjekk om omsorgspenger-tilgangsstyring")
        }
    }

    override suspend fun check() = pingUri.httpGet().second.fold(
        onSuccess = { response ->
            when (HttpStatusCode.OK == response.status) {
                true -> Healthy(Navn, "OK")
                false -> UnHealthy(Navn, "Feil: Mottok Http Status Code ${response.status.value}")
            }
        },
        onFailure = {
            UnHealthy(Navn, "Feil: ${it.message}")
        }
    )

    private companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerTilgangsstyringGateway::class.java)
        private const val Navn = "OmsorgspengerTilgangsstyringGateway"
        private fun Operasjon.somJsonBody() = """
            {
                "identitetsnummer": ${identitetsnummer.map { """"$it"""" }},
                "beskrivelse": "$beskrivelse",
                "operasjon": "${type.name}"
            }
            """.trimIndent()
    }
}