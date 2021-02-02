package no.nav.omsorgsdager.aksjonspunkt

import no.nav.omsorgsdager.Json

internal interface LøstAksjonpunkt {
    val navn: String
    val versjon : String
    val kanFastsettes : Boolean
    val løsning: Json
}