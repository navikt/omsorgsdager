package no.nav.omsorgsdager.behov

internal data class Behov (
    internal val uløsteBehov: Set<UløstBehov>,
    internal val løsteBehov: Set<LøstBehov>,
    internal val alleBehovNavn: Set<String> = uløsteBehov.map { it.navn }.plus(løsteBehov.map { it.navn }).toSet()
)