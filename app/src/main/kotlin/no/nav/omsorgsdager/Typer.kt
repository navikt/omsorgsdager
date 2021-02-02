package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import org.json.JSONObject
import java.util.*

internal typealias CorrelationId = String
internal fun ApplicationCall.correlationId() = when {
    request.headers.contains(HttpHeaders.XCorrelationId) -> request.headers[HttpHeaders.XCorrelationId]!!
    request.headers.contains("Nav-Call-Id") -> request.headers["Nav-Call-Id"]!!
    else -> "omsorgsdager-${UUID.randomUUID()}"
}

internal typealias Saksnummer = String
internal typealias BehandlingId = String
internal fun ApplicationCall.behandlingId() : BehandlingId = parameters.getOrFail("behandlingId")
internal typealias Identitetsnummer = String

internal class Json private constructor(json: String) {
    internal val raw = JSONObject(json).toString()
    internal companion object {
        internal fun String.somJson() = Json(json = this)
        internal fun ObjectNode.somJson() = Json(json = toString())
    }
}