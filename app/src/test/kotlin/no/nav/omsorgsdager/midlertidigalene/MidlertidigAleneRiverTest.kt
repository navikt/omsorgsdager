package no.nav.omsorgsdager.midlertidigalene

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.MockedPersonInfoGateway
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class MidlertidigAleneRiverTest(
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
        val annenForeldersAktørId = "29099011114".somAktørId()

        val melding = MidlertidigAleneMeldinger.melding(
            behandlingId = behandlingId,
            søkersAktørId = søkerssAktørId,
            annenForeldersAktørId = annenForeldersAktørId
        )
        val (_, behovssekvens) = MidlertidigAleneMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkerssAktørId.somMockedIdentetsnummer(), annenForeldersAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("InnvilgetMidlertidigAlene")

        val behandling = applicationContext.behandlingService.hentAlle(behandlingId = behandlingId.somK9BehandlingId()).first()
        assertNotNull(behandling)
        assertTrue(behandling is MidlertidigAleneBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling.k9behandlingId)
        assertEquals(BehandlingStatus.INNVILGET, behandling.status)
    }

    @Test
    fun `avslått midlertidig alene`() {
        val behandlingId = "${K9BehandlingId.generateK9BehandlingId()}"
        val søkersAktørId = "29099011115".somAktørId()
        val annenForeldersAktørId = "29099011116".somAktørId()

        val melding = MidlertidigAleneMeldinger.melding(
            behandlingId = behandlingId,
            søkersAktørId = søkersAktørId,
            annenForeldersAktørId = annenForeldersAktørId
        )

        val (_, behovssekvens) = MidlertidigAleneMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersAktørId.somMockedIdentetsnummer(), annenForeldersAktørId.somMockedIdentetsnummer()))
        rapid.sisteMeldingHarLøsningPå("AvslåttMidlertidigAlene")

        val behandling = applicationContext.behandlingService.hentAlle(behandlingId = behandlingId.somK9BehandlingId()).first()
        assertNotNull(behandling)
        assertTrue(behandling is MidlertidigAleneBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling.k9behandlingId)
        assertEquals(BehandlingStatus.AVSLÅTT, behandling.status)
    }
}