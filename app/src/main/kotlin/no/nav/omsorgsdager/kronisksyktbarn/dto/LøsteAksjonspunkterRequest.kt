package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.aksjonspunkt.LøstAksjonpunkt

internal data class LegeerklæringAksjonspunktLøsning(
    val barnetErKroniskSykt: Boolean,
    val barnetErFunksjonshemmet: Boolean,
    val begrunnelse: String
) : LøstAksjonpunkt {
    override val navn = "LEGEERKLÆRING"
    override val versjon = "0.0.1"
    override val kanFastsettes = barnetErKroniskSykt || barnetErFunksjonshemmet
    override val løsning = """
        {
            "barnetErKroniskSykt": $barnetErKroniskSykt,
            "barnetErFunksjonshemmet": $barnetErFunksjonshemmet,
            "begrunnelse": "$begrunnelse"
        }
    """.trimIndent().somJson()
}


internal data class LøsteAksjonspunkterRequest(
    @get:JsonProperty("LEGEERKLÆRING") val LEGEERKLÆRING: LegeerklæringAksjonspunktLøsning?) {
    internal val løsteAksjonspunkter : Set<LøstAksjonpunkt> = setOf(LEGEERKLÆRING).filterNotNull().toSet()
    init {
        require(løsteAksjonspunkter.isNotEmpty()) {
            "Minst ett aksjonspunt må løses."
        }
    }

}