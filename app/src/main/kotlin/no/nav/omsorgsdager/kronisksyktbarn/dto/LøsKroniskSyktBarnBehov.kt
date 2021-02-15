package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.Fritekst
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.lovverk.Lovanvendelser
import no.nav.omsorgsdager.lovverk.Folketrygdeloven

internal object LøsKroniskSyktBarnBehov {

    /*
        - Er det dokumentert at barnets sykdom er kronisk eller at barnet har en funksjonshemming? Ja/nei
        - Er det en sammenheng mellom barnets kroniske sykdom eller funksjonshemming og søkers risiko for fravær fra arbeid? Ja/nei
     */
    internal data class VurdereKroniskSyktBarn(
        val barnetErKroniskSyktEllerHarEnFunksjonshemning: Boolean?,
        val erSammenhengMedSøkersRisikoForFraværFraArbeid: Boolean?,
        val vurdering: String
    ) : LøstBehov {
        override val navn = "VURDERE_KRONISK_SYKT_BARN"
        override val versjon = 1
        override val lovanvendelser = {
            val builder = Lovanvendelser.Builder()
            if (requireNotNull(barnetErKroniskSyktEllerHarEnFunksjonshemning) { "barnetErKroniskSyktEllerHarEnFunksjonshemning må settes" }) {
                builder.innvilget(Folketrygdeloven.KroniskSyktBarn, "Barnet er kronisk sykt eller har en funksjonshemning.")
            } else {
                builder.avslått(Folketrygdeloven.KroniskSyktBarn, "Barnet er hverken kronisk sykt eller har en funksjonshemning.")
            }
            if (requireNotNull(erSammenhengMedSøkersRisikoForFraværFraArbeid) { "erSammenhengMedSøkersRisikoForFraværFraArbeid må settes" }) {
                builder.innvilget(Folketrygdeloven.KroniskSyktBarn, "Er sammenheng med søkers risiko for fravær fra arbeidet.")
            } else {
                builder.avslått(Folketrygdeloven.KroniskSyktBarn, "Er ikke sammenheng med søkers risiko for fravær fra arbeidet.")
            }
            builder.build()
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
        @get:JsonProperty("VURDERE_KRONISK_SYKT_BARN") val VURDERE_KRONISK_SYKT_BARN: VurdereKroniskSyktBarn?) {
        internal val løsteBehov : Set<LøstBehov> = setOf(VURDERE_KRONISK_SYKT_BARN).filterNotNull().toSet()
        init {
            require(løsteBehov.isNotEmpty()) {
                "Minst ett behov må løses."
            }
        }
    }
}