package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.Issuer
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes
import no.nav.helse.dusseldorf.ktor.core.DefaultStatusPages
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.omsorgsdager.config.hentRequiredEnv
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnRoute
import java.net.URI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
internal fun Application.app(
    applicationContext: ApplicationContext = ApplicationContext.Builder().build()
) {

    /*Flyway.configure()
        .dataSource(applicationContext.dataSource)
        .load()
        .migrate()
    */
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
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
        multipleJwtIssuers(issuers)
    }

    HealthReporter(
        app = "omsorgsdager",
        healthService = applicationContext.healthService
    )


    install(Routing) {
        HealthRoute(healthService = applicationContext.healthService)
        DefaultProbeRoutes()
        authenticate(*issuers.allIssuers()) {
            route("/api") {
                KroniskSyktBarnRoute(
                    tilgangsstyring = applicationContext.tilgangsstyring,
                    kafkaProducer = applicationContext.kafkaProducer,
                    utvidettRepository = applicationContext.utvidettRepository
                )
            }
        }
    }

}

