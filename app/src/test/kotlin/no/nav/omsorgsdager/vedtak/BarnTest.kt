package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Gjeldende
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.Barn.Companion.sammenlignPå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class BarnTest {

    @Test
    fun `Samme barn med forskjellig kombinasjon av ider håndteres som samme barn`() {
        val identitetsnummer = "11111111111".somIdentitetsnummer()
        val fødselsdato = "2020-01-05".dato()
        val saksnummer = identitetsnummer.somMocketOmsorgspengerSaksnummer()

        val barnK9Sak = Barn(identitetsnummer = identitetsnummer, omsorgspengerSaksnummer = saksnummer, fødselsdato = fødselsdato)
        val barnInfotrygdMedId = Barn(identitetsnummer = identitetsnummer, fødselsdato = fødselsdato)
        val barnInfotrygdMedKunFødselsdato = Barn(identitetsnummer = null, fødselsdato = fødselsdato)

        val barna = listOf(barnK9Sak, barnInfotrygdMedId, barnInfotrygdMedKunFødselsdato).sammenlignPå()

        val gjortSist = barnInfotrygdMedKunFødselsdato.somTestGjeldende(3, barna)
        val gjeldende = listOf(
            barnK9Sak.somTestGjeldende(1, barna),
            barnInfotrygdMedId.somTestGjeldende(2, barna),
            gjortSist
        ).gjeldende()


        assertThat(gjeldende).containsOnly(gjortSist)

    }

    @Test
    fun `Samme fødselsdato forskjellige identietsnummer`() {
        val identitetsnummer1 = "11111111111".somIdentitetsnummer()
        val identitetsnummer2 = "11111111111".somIdentitetsnummer()
        val fødselsdato = "2020-01-05".dato()

        val barnInfotrygd1 = Barn(identitetsnummer = identitetsnummer1, fødselsdato = fødselsdato)
        val barnInfotrygd2 = Barn(identitetsnummer = identitetsnummer2, fødselsdato = fødselsdato)


        // Om èn
    }

    private companion object {
        private val tidspunktet = ZonedDateTime.now()
        private val perioden = Periode(2021)

        fun Barn.somTestGjeldende(tidspunktPluss: Int, sammenlignPå: (barn: Barn) -> Any) = TestGjeldende(barn = this, tidspunktPluss = tidspunktPluss, enPer = sammenlignPå(this))

        data class TestGjeldende(
            private val tidspunktPluss: Int,
            override val periode: Periode = perioden,
            internal val barn: Barn,
            override val enPer: Any) : Gjeldende.KanUtledeGjeldende {
            override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(periode = nyPeriode)
            override val tidspunkt: ZonedDateTime = tidspunktet.plusMinutes(tidspunktPluss.toLong())
        }
    }
}