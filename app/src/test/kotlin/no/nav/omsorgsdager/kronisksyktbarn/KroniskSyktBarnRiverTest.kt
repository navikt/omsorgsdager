package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class KroniskSyktBarnRiverTest(
    private val builder: ApplicationContext.Builder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.buildStarted())
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

    @Test
    fun `håndterer at en melding med samme behandlingId kan kommer flere ganger`() {
        val søkersIdentitetsnummer = "29099111111"
        val behandlingId = "${UUID.randomUUID()}"

        // Første melding alt OK
        val melding = KroniskSyktBarnMeldinger.melding(
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            behandlingId = behandlingId
        )
        val (_, behovssekvens) = KroniskSyktBarnMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("InnvilgetKroniskSyktBarn")

        // Samme grunnlag men ny status skal feile
        val (_, behovssekvens2) = KroniskSyktBarnMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens2)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer))
        rapid.sisteMeldingManglerLøsningPå("InnvilgetKroniskSyktBarn")
        rapid.sisteMeldingManglerLøsningPå("AvslåttKroniskSyktBarn")

        // Samme grunnlag og samme status skal gå OK
        val (_, behovssekvens3) = KroniskSyktBarnMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens3)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("InnvilgetKroniskSyktBarn")


        // Nytt grunnlag vil feile
        val melding2 = KroniskSyktBarnMeldinger.melding(
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            behandlingId = behandlingId
        )

        val (_, behovssekvens4) = KroniskSyktBarnMeldinger.innvilget(melding2).keyValue

        rapid.sendTestMessage(behovssekvens4)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer))
        rapid.sisteMeldingManglerLøsningPå("InnvilgetKroniskSyktBarn")
    }
}