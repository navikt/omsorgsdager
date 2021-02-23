package no.nav.omsorgsdager.midlertidigalene

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class MidlertidigAleneRiverTest(
    private val builder: ApplicationContext.Builder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.buildStarted())
    }

    @Test
    fun `innvilget kronisk sykt barn`() {
        val søkersIdentitetsnummer = "29099011113"
        val annenForelderIdentitetsnummer = "29099011114"

        val melding = MidlertidigAleneMeldinger.melding(
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            annenForelderIdentitetsnummer = annenForelderIdentitetsnummer
        )
        val (_, behovssekvens) = MidlertidigAleneMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer, annenForelderIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("InnvilgetMidlertidigAlene")
    }

    @Test
    fun `avslått kronisk sykt barn`() {
        val søkersIdentitetsnummer = "29099011115"
        val annenForelderIdentitetsnummer = "29099011116"

        val melding = MidlertidigAleneMeldinger.melding(
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            annenForelderIdentitetsnummer = annenForelderIdentitetsnummer
        )
        val (_, behovssekvens) = MidlertidigAleneMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer, annenForelderIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("AvslåttMidlertidigAlene")
    }
}