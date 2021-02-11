package no.nav.omsorgsdager.lovverk

import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import org.json.JSONObject

internal data class Lovanvendelser (
    internal val innvilget: Map<String, Set<String>> = emptyMap(),
    internal val avslått: Map<String, Set<String>> = emptyMap()) {
    internal val json = JSONObject(mapOf(
        "innvilget" to innvilget,
        "avslått" to avslått
    )).somJson()

    // TODO, gjøre dette på en smidigere måte...
    internal constructor(json: Json) : this(
        innvilget = json.map["innvilget"] as Map<String, Set<String>>,
        avslått = json.map["avslått"] as Map<String, Set<String>>
    )

    internal class Builder {
        private val innvilget = mutableMapOf<String, MutableSet<String>>()
        private val avslått = mutableMapOf<String, MutableSet<String>>()
        internal fun innvilget(lovhenvisning: String, lovanvendelse: String) : Builder {
            innvilget[lovhenvisning] = (innvilget[lovhenvisning] ?: mutableSetOf()).plus(lovanvendelse).toMutableSet()
            return this
        }

        internal fun avslått(lovhenvisning: String, lovanvendelse: String) : Builder {
            avslått[lovhenvisning] = (avslått[lovhenvisning] ?: mutableSetOf()).plus(lovanvendelse).toMutableSet()
            return this
        }

        internal fun build() = Lovanvendelser(
            innvilget = innvilget,
            avslått = avslått
        )
    }
}