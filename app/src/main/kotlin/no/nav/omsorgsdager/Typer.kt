package no.nav.omsorgsdager

import de.huxhorn.sulky.ulid.ULID
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import java.util.*

internal fun requireNoException(block: () -> Any?, error: String) = runCatching { block() }.fold(
    onSuccess = { requireNotNull(it) { error } },
    onFailure = { throw IllegalArgumentException(error, it) }
)

internal typealias BehandlingId = Long

internal data class CorrelationId private constructor(private val value: String) {
    init {
        require(value.matches(Regex)) { "$value er en ugyldig correlation id" }
    }

    override fun toString() = value

    internal companion object {
        private val Regex = "[a-zA-Z0-9_.\\-æøåÆØÅ]{5,200}".toRegex()
        internal fun genererCorrelationId() = CorrelationId("omsorgsdager-${UUID.randomUUID()}")
        internal fun String.somCorrelationId() = CorrelationId(this)
        internal fun ApplicationCall.correlationId() = requireNotNull(callId).somCorrelationId()
    }
}

internal data class BehovssekvensId private constructor(private val value: String) {
    init {
        requireNoException(error = "$value er ikke en gyldig BehovssekvensId", block = { ULID.parseULID(value) })
    }

    override fun toString() = value

    internal companion object {
        private val Ulid = ULID()
        internal fun String.somBehovssekvensId() = BehovssekvensId(this)
        internal fun genererBehovssekvensId() = Ulid.nextULID().somBehovssekvensId()
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
        require(value.matches(Regex)) { "$value er et ugyldig omsorgspenger saksnummer" }
    }

    override fun toString() = value

    internal companion object {
        private val Regex = "[A-Za-z0-9]{5,20}".toRegex()
        internal fun String.somOmsorgspengerSaksnummer() = OmsorgspengerSaksnummer(this)
    }
}

internal data class K9Saksnummer private constructor(private val value: String) {
    init {
        require(value.matches(Regex)) { "$value er et ugyldig K9 saksnummer" }
    }

    override fun toString() = value

    internal companion object {
        private val Regex = "[A-Za-z0-9]{5,20}".toRegex()
        internal fun String.somK9Saksnummer() = K9Saksnummer(this)
    }
}

internal data class K9BehandlingId private constructor(private val value: String) {
    init {
        requireNoException(error = "$value er ikke en gyldig K9 behandlingId", block = { UUID.fromString(value) })
    }

    override fun toString() = value

    internal companion object {
        internal fun generateK9BehandlingId() = K9BehandlingId("${UUID.randomUUID()}")
        internal fun String.somK9BehandlingId() = K9BehandlingId(this)
    }
}