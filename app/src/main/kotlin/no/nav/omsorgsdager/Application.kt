package no.nav.omsorgsdager

import io.ktor.application.*
import io.ktor.server.cio.*
import no.nav.helse.rapids_rivers.KtorBuilder
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.k9.rapid.river.hentOptionalEnv
import org.slf4j.LoggerFactory

fun main() {
    val applicationContext = ApplicationContext.Builder().build()

    when (applicationContext.env.hentOptionalEnv("APP_MODE")) {
        "API_ONLY" -> KtorBuilder()
            .log(LoggerFactory.getLogger("no.nav.omsorgdager.api_only"))
            .port(applicationContext.env.hentOptionalEnv("HTTP_PORT")?.toInt() ?: 8080)
            .liveness { true }
            .readiness { true }
            .module {
                omsorgsdager(applicationContext)
                environment.monitor.subscribe(ApplicationStarted) { applicationContext.start() }
            }
            .build(CIO)
            .start(wait = false)

        else -> RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
            .withKtorModule { omsorgsdager(applicationContext) }
            .build()
            .apply { registerApplicationContext(applicationContext) }
            .start()
    }
}