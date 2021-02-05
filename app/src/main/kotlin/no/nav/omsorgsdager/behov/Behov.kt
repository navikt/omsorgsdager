package no.nav.omsorgsdager.behov

internal data class Behov (
    internal val uløsteBehov: Set<UløstBehov>,
    internal val løsteBehov: Set<LøstBehov>)