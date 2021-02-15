package no.nav.omsorgsdager.behov.dto

import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.behov.UløstBehov

internal object HentBehov {
    internal data class LøstBehovValue(
        val løsning: Map<String, Any?>,
        val lovanvendelser: Map<String, Any?>
    )

    internal fun Set<UløstBehov>.uløsteDto() = associateBy { it.navn }.mapValues { Any() }
    internal fun Set<LøstBehov>.løsteDto() = associateBy { it.navn }.mapValues { LøstBehovValue(
        løsning = it.value.løsning.map,
        lovanvendelser = it.value.lovanvendelser.somJson().map
    )}

}