package no.nav.omsorgsdager.lovverk

import no.nav.omsorgsdager.Json

internal data class Lovanvendelser (
    internal val innvilget: Map<String, List<String>> = emptyMap(),
    internal val avslått: Map<String, List<String>> = emptyMap()) {

    // TODO
    internal constructor(json: Json) : this(
        innvilget = emptyMap(),
        avslått = emptyMap()
    )

    internal class Builder {
        private val innvilget = mutableMapOf<String, MutableList<String>>()
        private val avslått = mutableMapOf<String, MutableList<String>>()
        internal fun innvilget(lovhenvisning: String, lovanvendelse: String) : Builder {
            //innvilget.put(lovanvendelse, (innvilget[lovanvendelse]?: mutableListOf()).add(""))
            return this
        }

        internal fun avslått(lovhenvisning: String, lovanvendelse: String) : Builder {
            return this
        }

        internal fun build() = Lovanvendelser(
            innvilget = innvilget,
            avslått = avslått
        )
    }
}