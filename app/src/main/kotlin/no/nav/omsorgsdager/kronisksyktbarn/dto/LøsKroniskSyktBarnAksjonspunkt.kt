package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.Fritekst
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.aksjonspunkt.LøstAksjonpunkt

internal object LøsKroniskSyktBarnAksjonspunkt {

    /*
        - Er det dokumentert at barnets sykdom er kronisk eller at barnet har en funksjonshemming? Ja/nei
        - Er det en sammenheng mellom barnets kroniske sykdom eller funksjonshemming og søkers risiko for fravær fra arbeid? Ja/nei
     */
    internal data class Legeerklæring(
        val barnetErKroniskSyktEllerHarEnFunksjonshemning: Boolean,
        val erSammenhengMedSøkersRisikoForFraværeFraArbeid: Boolean,
        val vurdering: String
    ) : LøstAksjonpunkt {
        override val navn = "LEGEERKLÆRING"
        override val versjon = "0.0.1"
        override val kanFastsettes = barnetErKroniskSyktEllerHarEnFunksjonshemning && erSammenhengMedSøkersRisikoForFraværeFraArbeid
        override val løsning = """
        {
            "barnetErKroniskSyktEllerHarEnFunksjonshemning": $barnetErKroniskSyktEllerHarEnFunksjonshemning,
            "erSammenhengMedSøkersRisikoForFraværeFraArbeid": $erSammenhengMedSøkersRisikoForFraværeFraArbeid,
            "vurdering": "${Fritekst(vurdering).tekst}"
        }
        """.trimIndent().somJson()
    }

    internal data class Request(
        @get:JsonProperty("LEGEERKLÆRING") val LEGEERKLÆRING: Legeerklæring?) {
        internal val løsteAksjonspunkter : Set<LøstAksjonpunkt> = setOf(LEGEERKLÆRING).filterNotNull().toSet()
        init {
            require(løsteAksjonspunkter.isNotEmpty()) {
                "Minst ett aksjonspunt må løses."
            }
        }
    }
}