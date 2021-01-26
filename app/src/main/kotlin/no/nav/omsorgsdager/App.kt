package no.nav.omsorgsdager

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes
import no.nav.omsorgsdager.utvidetrett.UtvidetRett

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
internal fun Application.app(
    applicationContext: ApplicationContext = ApplicationContext.Builder().build()
) {

    install(ContentNegotiation) {
        jackson()
    }


    install(Routing) {
        DefaultProbeRoutes()
        UtvidetRett()
    }

}

