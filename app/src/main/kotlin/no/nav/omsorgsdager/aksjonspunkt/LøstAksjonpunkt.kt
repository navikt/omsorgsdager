package no.nav.omsorgsdager.aksjonspunkt

import no.nav.omsorgsdager.Json

internal interface LøstAksjonpunkt {
    val navn: String
    val versjon : String
    val kanInnvilges : Boolean
    val løsning: Json
}

internal fun Collection<LøstAksjonpunkt>.kanInnvilges() = all { it.kanInnvilges }