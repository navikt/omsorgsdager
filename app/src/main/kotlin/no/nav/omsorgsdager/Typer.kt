package no.nav.omsorgsdager

import io.ktor.application.*
import io.ktor.http.*
import java.util.*

internal typealias BehandlingId = Long

internal data class CorrelationId private constructor(private val value: String) {
    init {
        // TODO: Valider
    }
    override fun toString() = value
    internal companion object {
        internal fun genererCorrelationId() = CorrelationId("omsorgsdager-${UUID.randomUUID()}")
        internal fun ApplicationCall.correlationId() = when {
            request.headers.contains(HttpHeaders.XCorrelationId) -> request.headers[HttpHeaders.XCorrelationId]!!.let { CorrelationId(it) }
            request.headers.contains("Nav-Call-Id") -> request.headers["Nav-Call-Id"]!!.let { CorrelationId(it) }
            else -> genererCorrelationId()
        }
    }
}


internal data class Identitetsnummer private constructor(private val value: String) {
    init {
        require(value.matches(Regex)) { "Ugyldig identitetsnummer" }
    }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{11,25}".toRegex()
        internal fun String.somIdentitetsnummer() = Identitetsnummer(this)
    }
}

internal data class OmsorgspengerSaksnummer private constructor(private val value: String) {
    init {
        // TODO: Valider
    }
    override fun toString() = value
    internal companion object {
        internal fun String.somOmsorgspengerSaksnumer() = OmsorgspengerSaksnummer(this)
    }
}

internal data class K9Saksnummer private constructor(private val value: String) {
    init {
        // TODO: Valider
    }
    override fun toString() = value
    internal companion object {
        internal fun String.somK9Saksnummer() = K9Saksnummer(this)
    }
}

internal data class K9BehandlingId private constructor(private val value: String) {
    init {
        requireNotNull(UUID.fromString(value))
    }
    override fun toString() = value
    internal companion object {
        internal fun String.somK9BehandlingId() = K9BehandlingId(this)
    }
}