package no.nav.omsorgsdager.AleneOmsorg

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.aleneomsorg.AleneOmsorgBehandling
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.MockedPersonInfoGateway
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class AleneOmsorgRiverTest(
    builder: ApplicationContext.Builder) {
    private val applicationContext = builder.also {
        it.personInfoGatway = MockedPersonInfoGateway()
    }.buildStarted()

    private val rapid = TestRapid().also {
        it.registerApplicationContext(applicationContext)
    }

    @Test
    fun `innvilget midlertidig alene`() {
        val behandlingId = "${K9BehandlingId.generateK9BehandlingId()}"
        val søkerssAktørId = "29099011113".somAktørId()
        val barnsAktørId = "29099011114".somAktørId()

        val melding = AleneOmsorgMeldinger.melding(
            behandlingId = behandlingId,
            søkersAktørId = søkerssAktørId,
            barnsAktørId = barnsAktørId
        )
        val (_, behovssekvens) = AleneOmsorgMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkerssAktørId.somMockedIdentetsnummer(), barnsAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("InnvilgetAleneOmsorg")

        val behandling = applicationContext.behandlingService.hentAlle(behandlingId = behandlingId.somK9BehandlingId()).first()
        assertNotNull(behandling)
        assertTrue(behandling is AleneOmsorgBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling.k9behandlingId)
        assertEquals(BehandlingStatus.INNVILGET, behandling.status)
    }

    @Test
    fun `innvilget midlertidig alene til tidenes ende`() {
        val behandlingId = "${K9BehandlingId.generateK9BehandlingId()}"
        val søkerssAktørId = "29099011113".somAktørId()
        val barnsAktørId = "29099011114".somAktørId()

        val melding = AleneOmsorgMeldinger.melding(
            behandlingId = behandlingId,
            søkersAktørId = søkerssAktørId,
            barnsAktørId = barnsAktørId,
            periode = Periode("2020-01-01/9999-12-31")
        )
        val (_, behovssekvens) = AleneOmsorgMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkerssAktørId.somMockedIdentetsnummer(), barnsAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("InnvilgetAleneOmsorg")

        val behandling = applicationContext.behandlingService.hentAlle(behandlingId = behandlingId.somK9BehandlingId()).first()
        assertNotNull(behandling)
        assertTrue(behandling is AleneOmsorgBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling.k9behandlingId)
        assertEquals(BehandlingStatus.INNVILGET, behandling.status)
    }

    @Test
    fun `avslått midlertidig alene`() {
        val behandlingId = "${K9BehandlingId.generateK9BehandlingId()}"
        val søkersAktørId = "29099011115".somAktørId()
        val barnsAktørId = "29099011116".somAktørId()

        val melding = AleneOmsorgMeldinger.melding(
            behandlingId = behandlingId,
            søkersAktørId = søkersAktørId,
            barnsAktørId = barnsAktørId
        )

        val (_, behovssekvens) = AleneOmsorgMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer(), barnsAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("AvslåttAleneOmsorg")

        val behandling = applicationContext.behandlingService.hentAlle(behandlingId = behandlingId.somK9BehandlingId()).first()
        assertNotNull(behandling)
        assertTrue(behandling is AleneOmsorgBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling.k9behandlingId)
        assertEquals(BehandlingStatus.AVSLÅTT, behandling.status)
    }
}