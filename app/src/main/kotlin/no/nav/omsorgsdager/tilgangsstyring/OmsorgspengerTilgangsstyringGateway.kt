package no.nav.omsorgsdager.tilgangsstyring

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.toByteArray
import java.util.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import org.slf4j.LoggerFactory
import java.net.URI

internal class OmsorgspengerTilgangsstyringGateway(
    private val httpClient: HttpClient,
    omsorgspengerTilgangsstyringUri : URI
): HealthCheck {
    private val personTilgangUri = "$omsorgspengerTilgangsstyringUri/api/tilgang/personer"
    private val isAliveUri = "$omsorgspengerTilgangsstyringUri/isalive"

    internal suspend fun harTilgang(
        token: Token,
        operasjon: Operasjon): Boolean {
        return kotlin.runCatching {
            httpClient.post<HttpStatement>(personTilgangUri) {
                header(HttpHeaders.Authorization, token.authorizationHeader)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString()) // TODO
                body = PersonerRequestBody(
                    identitetsnummer = operasjon.identitetsnummer,
                    beskrivelse = operasjon.beskrivelse,
                    operasjon = operasjon.type.name
                )
            }.execute()
        }.håndterResponse()
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

    override suspend fun check(): no.nav.helse.dusseldorf.ktor.health.Result {
        return kotlin.runCatching {
            httpClient.get<HttpStatement>(isAliveUri).execute()
        }.fold(
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
    }

    private companion object {
        private const val Navn = "OmsorgspengerTilgangsstyringGateway"
        private val logger = LoggerFactory.getLogger(OmsorgspengerTilgangsstyringGateway::class.java)
        private class PersonerRequestBody(
            val identitetsnummer: Set<String>,
            val operasjon: String,
            val beskrivelse: String
        )
    }
}