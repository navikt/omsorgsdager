package no.nav.omsorgsdager

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.*
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgsdager.Json.Companion.configured
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakApis
import java.net.URI

internal fun Application.omsorgsdager(
    applicationContext: ApplicationContext = ApplicationContext.Builder().build()
) {

    install(ContentNegotiation) {
        jackson {
            configured()
        }
    }

    install(StatusPages) {
        DefaultStatusPages()
        AuthStatusPages()
    }

    val azureV2 = Issuer(
        issuer = applicationContext.env.hentRequiredEnv("AZURE_OPENID_CONFIG_ISSUER"),
        jwksUri = URI(applicationContext.env.hentRequiredEnv("AZURE_OPENID_CONFIG_JWKS_URI")),
        audience = applicationContext.env.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
        alias = "azure-v2"
    )

    val issuers = mapOf(
        azureV2.alias() to azureV2,
    ).withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    applicationContext.configure(this)

    val healthService = HealthService(
        healthChecks = applicationContext.healthChecks.plus(object : HealthCheck {
            override suspend fun check(): Result {
                val currentState = applicationContext.rapidsState
                return when (currentState.isHealthy()) {
                    true -> Healthy("RapidsConnection", currentState.asMap)
                    false -> UnHealthy("RapidsConnection", currentState.asMap)
                }
            }
        })
    )

    HealthReporter(
        app = "omsorgsdager",
        healthService = healthService
    )

    preStopOnApplicationStopPreparing(
        preStopActions = listOf(
            FullførAktiveRequester(this)
        )
    )

    install(CallId) {
        fromXCorrelationIdHeader(
            generateOnInvalid = true
        )
    }

    install(CallLogging) {
        logRequests()
        correlationIdAndRequestIdInMdc()
        callIdMdc("callId")
    }

    routing {
        HealthRoute(healthService = healthService)
        authenticate(*issuers.allIssuers()) {
            route("/api") {
                InnvilgedeVedtakApis(
                    tilgangsstyring = applicationContext.tilgangsstyring,
                    innvilgedeVedtakService = applicationContext.innvilgedeVedtakService
                )
            }
        }
    }
}

