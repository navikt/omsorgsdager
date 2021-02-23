package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class KroniskSyktBarnRiverTest(
    private val builder: ApplicationContext.Builder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.build())
    }

    @Test
    fun `innvilget kronisk sykt barn`() {
        val søkersIdentitetsnummer = "29099011111"
        val melding = KroniskSyktBarnMeldinger.melding(
            søkersIdentitetsnummer = søkersIdentitetsnummer
        )
        val (_, behovssekvens) = KroniskSyktBarnMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("InnvilgetKroniskSyktBarn")
    }

    @Test
    fun `avslått kronisk sykt barn`() {
        val søkersIdentitetsnummer = "29099011112"
        val melding = KroniskSyktBarnMeldinger.melding(
            søkersIdentitetsnummer = søkersIdentitetsnummer
        )
        val (_, behovssekvens) = KroniskSyktBarnMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("AvslåttKroniskSyktBarn")
    }
}