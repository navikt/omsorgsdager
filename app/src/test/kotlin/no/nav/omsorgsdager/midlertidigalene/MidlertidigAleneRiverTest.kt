package no.nav.omsorgsdager.midlertidigalene

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.registerApplicationContext
import no.nav.omsorgsdager.testutils.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.rapid.mockHentOmsorgsdagerSaksnummer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class MidlertidigAleneRiverTest(
    builder: ApplicationContext.Builder) {
    private val applicationContext = builder.buildStarted()

    private val rapid = TestRapid().also {
        it.registerApplicationContext(applicationContext)
    }

    @Test
    fun `innvilget midlertidig alene`() {
        val behandlingId = "${UUID.randomUUID()}"
        val søkersIdentitetsnummer = "29099011113"
        val annenForelderIdentitetsnummer = "29099011114"

        val melding = MidlertidigAleneMeldinger.melding(
            behandlingId = behandlingId,
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            annenForelderIdentitetsnummer = annenForelderIdentitetsnummer
        )
        val (_, behovssekvens) = MidlertidigAleneMeldinger.innvilget(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer, annenForelderIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("InnvilgetMidlertidigAlene")

        val behandling = applicationContext.behandlingService.hentEn(behandlingId = behandlingId.somK9BehandlingId())
        assertNotNull(behandling)
        assertTrue(behandling is MidlertidigAleneBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling!!.k9behandlingId)
        assertEquals(BehandlingStatus.INNVILGET, behandling.status)
    }

    @Test
    fun `avslått midlertidig alene`() {
        val behandlingId = "${UUID.randomUUID()}"
        val søkersIdentitetsnummer = "29099011115"
        val annenForelderIdentitetsnummer = "29099011116"

        val melding = MidlertidigAleneMeldinger.melding(
            behandlingId = behandlingId,
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            annenForelderIdentitetsnummer = annenForelderIdentitetsnummer
        )

        val (_, behovssekvens) = MidlertidigAleneMeldinger.avslått(melding).keyValue
        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgsdagerSaksnummer(setOf(søkersIdentitetsnummer, annenForelderIdentitetsnummer))
        rapid.sisteMeldingHarLøsningPå("AvslåttMidlertidigAlene")

        val behandling = applicationContext.behandlingService.hentEn(behandlingId = behandlingId.somK9BehandlingId())
        assertNotNull(behandling)
        assertTrue(behandling is MidlertidigAleneBehandling)
        assertEquals(behandlingId.somK9BehandlingId(), behandling!!.k9behandlingId)
        assertEquals(BehandlingStatus.AVSLÅTT, behandling.status)
    }
}