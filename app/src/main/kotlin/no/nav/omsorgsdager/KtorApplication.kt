package no.nav.omsorgsdager

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.auth.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.omsorgsdager.CorrelationId.Companion.correlationId
import no.nav.omsorgsdager.Json.Companion.configured
import no.nav.omsorgsdager.config.hentRequiredEnv
import no.nav.omsorgsdager.tilgangsstyring.TokenResolver.Companion.token
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakApis
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.URI

private val appLogger = LoggerFactory.getLogger("no.nav.omsorgsdager.omsorgsdager")

internal fun Application.omsorgsdager(
    applicationContext: ApplicationContext = ApplicationContext.Builder().build()) {

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
        issuer = applicationContext.env.hentRequiredEnv("AZURE_V2_ISSUER"),
        jwksUri = URI(applicationContext.env.hentRequiredEnv("AZURE_V2_JWKS_URI")),
        audience = applicationContext.env.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
        alias = "azure-v2"
    )

    val openAm = Issuer(
        issuer = applicationContext.env.hentRequiredEnv("OPEN_AM_ISSUER"),
        jwksUri = URI(applicationContext.env.hentRequiredEnv("OPEN_AM_JWKS_URI")),
        audience = null,
        alias = "open-am"
    )

    val issuers = mapOf(
        azureV2.alias() to azureV2,
        openAm.alias() to openAm
    ).withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(
            issuers = issuers,
            extractHttpAuthHeader = { call -> when (val token = call.token()) {
                null -> null
                else -> HttpAuthHeader.Single("Bearer", token)
            }}
        )
    }

    applicationContext.configure(this)

    HealthReporter(
        app = "omsorgsdager",
        healthService = applicationContext.healthService
    )

    preStopOnApplicationStopPreparing(preStopActions = listOf(
        FullførAktiveRequester(this)
    ))

    install(CallId) { retrieve { it.correlationId().toString() } }

    install(CallLogging) {
        val ignorePaths = setOf("/isalive", "/isready", "/metrics")
        level = Level.INFO
        logger = appLogger
        filter { call -> !ignorePaths.contains(call.request.path().toLowerCase()) }
        callIdMdc("correlation_id")
        callIdMdc("callId")
    }

    install(Routing) {
        HealthRoute(healthService = applicationContext.healthService)
        DefaultProbeRoutes()
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

