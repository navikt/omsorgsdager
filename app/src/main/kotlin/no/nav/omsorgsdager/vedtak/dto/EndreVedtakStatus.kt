package no.nav.omsorgsdager.vedtak.dto

import io.ktor.application.*
import io.ktor.request.*
import no.nav.omsorgsdager.Json.Companion.somJsonOrNull
import no.nav.omsorgsdager.tid.Periode.Companion.utcNå
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal object EndreVedtakStatus {
    private val logger = LoggerFactory.getLogger(EndreVedtakStatus::class.java)

    internal suspend fun ApplicationCall.endreVedtakStatusTidspunkt() : ZonedDateTime  {
        val tidspunkt =  receiveOrNull<String>()
            ?.somJsonOrNull()
            ?.map
            ?.get("tidspunkt")
            ?.let { ZonedDateTime.parse(it.toString())}
        return when (tidspunkt) {
            null -> utcNå().also { logger.info("Tidspunkt ikke satt i requesten, bruker $it") }
            else -> tidspunkt.also { logger.info("Tidspunkt satt i requestem, $it") }
        }
    }
}