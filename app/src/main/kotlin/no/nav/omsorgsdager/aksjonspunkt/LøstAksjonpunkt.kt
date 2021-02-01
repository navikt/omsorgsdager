package no.nav.omsorgsdager.aksjonspunkt

import no.nav.omsorgsdager.Json

internal interface LøstAksjonpunkt {
    fun navn() : String
    fun versjon() : String
    fun kanFastsettes() : Boolean
    fun løsning(): Json
}