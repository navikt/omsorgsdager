package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.MockedPersonInfoGateway
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class KroniskSyktBarnRiverTest(
    private val builder: ApplicationContext.Builder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.also {
            it.personInfoGatway = MockedPersonInfoGateway()
        }.buildStarted())
    }

    @Test
    fun `innvilget kronisk sykt barn`() {
        val søkersAktørId = "29099011111".somAktørId()
        val barnetsAktørId = "29099011112".somAktørId()

        val melding = KroniskSyktBarnMeldinger.melding(
            søkersAktørId = søkersAktørId,
            barnetsAktørId = barnetsAktørId
        )
        val (_, behovssekvens) = KroniskSyktBarnMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer(), barnetsAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("InnvilgetKroniskSyktBarn")
    }

    @Test
    fun `avslått kronisk sykt barn`() {
        val søkersAktørId = "29099011113".somAktørId()
        val barnetsAtkørId = "29099011114".somAktørId()

        val melding = KroniskSyktBarnMeldinger.melding(
            søkersAktørId = søkersAktørId,
            barnetsAktørId = barnetsAtkørId
        )
        val (_, behovssekvens) = KroniskSyktBarnMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer(), barnetsAtkørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("AvslåttKroniskSyktBarn")
    }

    @Test
    fun `håndterer at en melding med samme behandlingId kan kommer flere ganger`() {
        val søkersAktørId = "29099011115".somAktørId()
        val barnetsAktørId = "29099011116".somAktørId()

        val behandlingId = "${K9BehandlingId.generateK9BehandlingId()}"

        // Første melding alt OK
        val melding = KroniskSyktBarnMeldinger.melding(
            søkersAktørId = søkersAktørId,
            behandlingId = behandlingId,
            barnetsAktørId = barnetsAktørId
        )
        val (_, behovssekvens) = KroniskSyktBarnMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer(), barnetsAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("InnvilgetKroniskSyktBarn")

        // Samme grunnlag men ny status skal feile
        val (_, behovssekvens2) = KroniskSyktBarnMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens2)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingManglerLøsningPå("InnvilgetKroniskSyktBarn")
        rapid.sisteMeldingManglerLøsningPå("AvslåttKroniskSyktBarn")

        // Samme grunnlag og samme status skal gå OK
        val (_, behovssekvens3) = KroniskSyktBarnMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens3)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer(), barnetsAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("InnvilgetKroniskSyktBarn")


        // Nytt grunnlag vil feile
        val melding2 = KroniskSyktBarnMeldinger.melding(
            søkersAktørId = søkersAktørId,
            behandlingId = behandlingId,
            barnetsAktørId = barnetsAktørId
        )

        val (_, behovssekvens4) = KroniskSyktBarnMeldinger.innvilget(melding2).keyValue

        rapid.sendTestMessage(behovssekvens4)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingManglerLøsningPå("InnvilgetKroniskSyktBarn")
    }
}