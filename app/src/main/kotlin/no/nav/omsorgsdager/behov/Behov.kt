package no.nav.omsorgsdager.behov

internal data class Behov (
    internal val uløsteBehov: Set<UløstBehov>,
    internal val løsteBehov: Set<LøstBehov>
)

internal fun Set<UløstBehov>.uløsteDto() = associateBy { it.navn }.mapValues { Any() }
internal fun Set<LøstBehov>.løsteDto() = associateBy { it.navn }.mapValues { it.value.løsning.map }