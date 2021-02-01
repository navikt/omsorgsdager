package no.nav.omsorgsdager

import io.ktor.application.*
import io.ktor.http.*
import org.json.JSONObject
import java.util.*

internal typealias CorrelationId = String
internal fun ApplicationCall.correlationId() = when {
    request.headers.contains(HttpHeaders.XCorrelationId) -> request.headers[HttpHeaders.XCorrelationId]!!
    request.headers.contains("Nav-Call-Id") -> request.headers["Nav-Call-Id"]!!
    else -> "omsorgsdager-${UUID.randomUUID()}"
}

typealias Saksnummer = String
typealias BehandlingId = String
typealias Identitetsnummer = String

internal data class Json (
    internal val rawJson: String) {
    init {
        JSONObject(rawJson)
    }
}