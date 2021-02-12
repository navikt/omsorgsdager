package no.nav.omsorgsdager

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import java.util.*

internal typealias CorrelationId = String
internal fun ApplicationCall.correlationId() = when {
    request.headers.contains(HttpHeaders.XCorrelationId) -> request.headers[HttpHeaders.XCorrelationId]!!
    request.headers.contains("Nav-Call-Id") -> request.headers["Nav-Call-Id"]!!
    else -> "omsorgsdager-${UUID.randomUUID()}"
}

internal typealias VedtakId = Long
internal typealias Saksnummer = String
internal fun ApplicationCall.saksnummer() : Saksnummer = parameters.getOrFail("saksnummer")
internal typealias BehandlingId = String
internal fun ApplicationCall.behandlingId() : BehandlingId = parameters.getOrFail("behandlingId")
internal typealias Identitetsnummer = String

internal class Fritekst(
    input: String) {
    internal val tekst = input
    init { require(input.length <= 4000 && input.matches(Regex)) }
    private companion object {
        private val Regex = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$".toRegex()
    }
}