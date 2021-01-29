package no.nav.omsorgsdager.tilgangsstyring

internal data class Operasjon(
    internal val type: Type,
    internal val beskrivelse: String,
    internal val identitetsnummer: Set<String>) {
    override fun toString() = when (type) {
        Type.Visning -> "Visning av $beskrivelse for ${identitetsnummer.size} personer."
        Type.Endring -> "Endring på $beskrivelse for ${identitetsnummer.size} personer."
    }

    internal enum class Type {
        Visning,
        Endring
    }
}