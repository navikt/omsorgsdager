package no.nav.omsorgsdager.behov

import no.nav.omsorgsdager.Json

internal interface LøstBehov {
    val navn: String
    val versjon : String
    val kanInnvilges : Boolean
    val løsning: Json
}

internal fun Collection<LøstBehov>.kanInnvilges() = all { it.kanInnvilges }