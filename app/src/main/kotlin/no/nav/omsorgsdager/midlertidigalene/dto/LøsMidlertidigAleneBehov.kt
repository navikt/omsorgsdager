package no.nav.omsorgsdager.midlertidigalene.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.Fritekst
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.lovverk.Folketrygdeloven
import no.nav.omsorgsdager.lovverk.Lovanvendelser
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.periodeOrNull
import java.time.LocalDate

internal object LøsMidlertidigAleneBehov {
    internal data class VurdereMidlertidigAlene(
        val erSøkerenMidlertidigAleneOmOmsorgen: Boolean?,
        val vurdering: String,
        val gyldigFraOgMed: LocalDate? = null,
        val gyldigTilOgMed: LocalDate? = null
    ) : LøstBehov {
        override val navn = "VURDERE_MIDLERTIDIG_ALENE"
        override val versjon = 1
        private val periode: Periode? = (gyldigFraOgMed to gyldigTilOgMed).periodeOrNull()
        override val lovanvendelser = {
            val builder = Lovanvendelser.Builder()
            if (requireNotNull(erSøkerenMidlertidigAleneOmOmsorgen) { "erSøkerenMidlertidigAleneOmOmsorgen må settes" }) {
                requireNotNull(periode) { "Må settes periode for når søkeren er midlertidig alene." }
                builder.innvilget(Folketrygdeloven.MidlertidigAlene, "Søkeren er midlertidig alene om omsorgen.")
            } else {
                builder.avslått(Folketrygdeloven.MidlertidigAlene, "Søkeren er ikke midlertidig alene om omsorgen.")
            }
            builder.build()
        }()

        override val grunnlag = Json.tomJson()

        override val løsning = """
        {
            "erSøkerenMidlertidigAleneOmOmsorgen": $erSøkerenMidlertidigAleneOmOmsorgen,
            "gyldigFraOgMed": "$gyldigFraOgMed",
            "gyldigTilOgMed": "$gyldigTilOgMed",
            "vurdering": "${Fritekst(vurdering).tekst}"
        }
        """.trimIndent().somJson()
    }

    internal data class Request(
        @get:JsonProperty("VURDERE_MIDLERTIDIG_ALENE") val VURDERE_MIDLERTIDIG_ALENE: VurdereMidlertidigAlene?) {
        internal val løsteBehov : Set<LøstBehov> = setOf(VURDERE_MIDLERTIDIG_ALENE).filterNotNull().toSet()
        init {
            require(løsteBehov.isNotEmpty()) {
                "Minst ett behov må løses."
            }
        }
    }
}