package no.nav.omsorgsdager.lovverk

import no.nav.omsorgsdager.Json

internal data class Lovanvendelser (
    val innvilget: Map<String, Set<String>>,
    val avslått: Map<String, Set<String>>) {
    internal fun somJson() = Json(this)

    internal companion object {
        internal fun fraJson(json: Json) = json.deserialize<Lovanvendelser>()
    }

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
