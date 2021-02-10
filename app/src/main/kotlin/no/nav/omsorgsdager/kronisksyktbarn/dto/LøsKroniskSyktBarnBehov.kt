package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.Fritekst
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.lovverk.Lovanvendelser

internal object LøsKroniskSyktBarnBehov {

    /*
        - Er det dokumentert at barnets sykdom er kronisk eller at barnet har en funksjonshemming? Ja/nei
        - Er det en sammenheng mellom barnets kroniske sykdom eller funksjonshemming og søkers risiko for fravær fra arbeid? Ja/nei
     */
    internal data class Legeerklæring(
        val barnetErKroniskSyktEllerHarEnFunksjonshemning: Boolean?,
        val erSammenhengMedSøkersRisikoForFraværFraArbeid: Boolean?,
        val vurdering: String
    ) : LøstBehov {
        override val navn = "LEGEERKLÆRING"
        override val versjon = 1
        private val kanInnvilges =
            requireNotNull(barnetErKroniskSyktEllerHarEnFunksjonshemning) { "barnetErKroniskSyktEllerHarEnFunksjonshemning må settes" } &&
            requireNotNull(erSammenhengMedSøkersRisikoForFraværFraArbeid) { "erSammenhengMedSøkersRisikoForFraværFraArbeid må settes" }
        override val lovanvendelser = {
           Lovanvendelser.Builder()
               // TODO
               .let { when (kanInnvilges) {
                   true -> it.innvilget("Foo", "Bar")
                   false -> it.avslått("Foo", "Bar")
               }}
               .build()
        }()

        override val løsning = """
        {
            "barnetErKroniskSyktEllerHarEnFunksjonshemning": $barnetErKroniskSyktEllerHarEnFunksjonshemning,
            "erSammenhengMedSøkersRisikoForFraværFraArbeid": $erSammenhengMedSøkersRisikoForFraværFraArbeid,
            "vurdering": "${Fritekst(vurdering).tekst}"
        }
        """.trimIndent().somJson()
    }

    internal data class Request(
        @get:JsonProperty("LEGEERKLÆRING") val LEGEERKLÆRING: Legeerklæring?) {
        internal val løsteBehov : Set<LøstBehov> = setOf(LEGEERKLÆRING).filterNotNull().toSet()
        init {
            require(løsteBehov.isNotEmpty()) {
                "Minst ett behov må løses."
            }
        }
    }
}