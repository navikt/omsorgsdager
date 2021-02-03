package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.*

internal typealias CorrelationId = String
internal fun ApplicationCall.correlationId() = when {
    request.headers.contains(HttpHeaders.XCorrelationId) -> request.headers[HttpHeaders.XCorrelationId]!!
    request.headers.contains("Nav-Call-Id") -> request.headers["Nav-Call-Id"]!!
    else -> "omsorgsdager-${UUID.randomUUID()}"
}

internal typealias Saksnummer = String
internal fun ApplicationCall.saksnummer() : Saksnummer = parameters.getOrFail("saksnummer")
internal typealias BehandlingId = String
internal fun ApplicationCall.behandlingId() : BehandlingId = parameters.getOrFail("behandlingId")
internal typealias Identitetsnummer = String

internal class Json private constructor(json: String) {
    private val jsonObject = JSONObject(json)
    internal val map = jsonObject.toMap()
    internal val raw = requireNotNull(jsonObject.toString()) {
        "Ugyldig JSON $json"
    }

    override fun equals(other: Any?) = when (other) {
        !is Json -> false
        else -> JSONCompare.compareJSON(raw, other.raw, JSONCompareMode.NON_EXTENSIBLE).passed()
    }

    override fun toString() = raw

    internal companion object {
        internal fun String.somJson() = Json(json = this)
        internal fun JSONObject.somJson() = Json(json = toString())
        internal fun ObjectNode.somJson() = Json(json = toString())
    }
}

internal class Fritekst(
    input: String) {
    internal val tekst = input
    init { require(input.length <= 4000 && input.matches(Regex)) }
    private companion object {
        private val Regex = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$".toRegex()
    }
}