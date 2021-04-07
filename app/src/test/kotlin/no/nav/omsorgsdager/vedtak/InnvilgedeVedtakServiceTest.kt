package no.nav.omsorgsdager.vedtak

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.omsorgsdager.*
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.behandling.NyBehandling
import no.nav.omsorgsdager.parter.Barn
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSakGateway
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import no.nav.omsorgsdager.vedtak.dto.Kilde
import no.nav.omsorgsdager.vedtak.dto.KroniskSyktBarnInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.infotrygd.KroniskSyktBarnInfotrygdInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(ApplicationContextExtension::class)
internal class InnvilgedeVedtakServiceTest(
    applicationContextBuilder: ApplicationContext.Builder) {

    private val nå = ZonedDateTime.now()

    private val mockedOmsorgspengerSakGateway = mockk<OmsorgspengerSakGateway>().also {
        coEvery { it.hentSaksnummer(Identitetsnummer1, any()) }.returns(OmsorgspengerSaksnummer1)
    }

    private val mockedOmsorgspengerInfotrygdRammevedtakGateway = mockk<OmsorgspengerInfotrygdRammevedtakGateway>().also {
        val kroniskSyktBarnInfotrygdVedtak = KroniskSyktBarnInfotrygdInnvilgetVedtak(
            vedtatt = nå.plusMinutes(1).toLocalDateOslo(),
            gyldigFraOgMed = LocalDate.parse("2020-05-05"),
            gyldigTilOgMed = LocalDate.parse("2033-12-31"),
            kilder = setOf(Kilde(id = "fra-infotrygd", type = "Infotrygd")),
            barnetsFødselsdato = LocalDate.parse("2020-01-01"),
            barnetsIdentitetsnummer = Identitetsnummer2
        )
        coEvery { it.hentInnvilgedeVedtak(Identitetsnummer1, any(), any()) }.returns(listOf(
            kroniskSyktBarnInfotrygdVedtak, kroniskSyktBarnInfotrygdVedtak.copy(
                barnetsIdentitetsnummer = "33333333333".somIdentitetsnummer(),
                barnetsFødselsdato = "2020-01-02".dato()
            )
        ))
    }

    private val applicationContext = applicationContextBuilder.also {
        it.omsorgspengerSakGateway = mockedOmsorgspengerSakGateway
        it.omsorgspengerInfotrygdRammevedtakGateway = mockedOmsorgspengerInfotrygdRammevedtakGateway
    }.buildStarted()

    @Test
    fun `Kombinerer innvilgede vedtak om kronisk sykt barn fra Infotrygd & K9-sak`() {
        val behandlingId1 = K9BehandlingId.generateK9BehandlingId()
        val behandlingId2 = K9BehandlingId.generateK9BehandlingId()
        val behandlingId3 = K9BehandlingId.generateK9BehandlingId()
        val behandlingId4 = K9BehandlingId.generateK9BehandlingId()


        val parter = listOf(
            Søker(identitetsnummer = Identitetsnummer1, omsorgspengerSaksnummer = OmsorgspengerSaksnummer1, aktørId = "11111".somAktørId()),
            Barn(identitetsnummer = Identitetsnummer2, omsorgspengerSaksnummer = OmsorgspengerSaksnummer2, fødselsdato = LocalDate.parse("2020-01-01"), aktørId = "22222".somAktørId())
        )

        val behandling1 = NyBehandling(
            behovssekvensId = BehovssekvensId.genererBehovssekvensId(),
            saksnummer = "12345".somK9Saksnummer(),
            behandlingId = behandlingId1,
            tidspunkt = nå,
            type = BehandlingType.KRONISK_SYKT_BARN,
            status = BehandlingStatus.INNVILGET,
            periode = Periode("2021-02-15/2032-12-31"),
            grunnlag = Json.tomJson()
        )
        val behandling2 = behandling1.copy(behandlingId = behandlingId2, periode = Periode("2033-01-01/2033-07-01"), tidspunkt = nå.plusMinutes(2))
        val behandling3 = behandling1.copy(behandlingId = behandlingId3, periode = Periode("2033-05-01/2033-07-01"), tidspunkt = nå.plusMinutes(3), status = BehandlingStatus.AVSLÅTT)
        val behandling4 = behandling1.copy(behandlingId = behandlingId4, periode = Periode("2020-05-05/2033-12-31"), tidspunkt = nå.minusWeeks(1), status = BehandlingStatus.AVSLÅTT)

        applicationContext.behandlingService.lagre(behandling1, parter)
        applicationContext.behandlingService.lagre(behandling2, parter)
        applicationContext.behandlingService.lagre(behandling3, parter)
        applicationContext.behandlingService.lagre(behandling4, parter) // Denne skal ikke ha noen innvirkning på resultatet ettersom tidspunktet er satt før både vedtatt i Infotrygd & før andre behandligner i K9-Sak

        val innvilgedeVedtakKroniskSyktBarn = hentInnvilgedeVedtak("11111111111".somIdentitetsnummer(), Periode("2020-01-01/2050-01-01")).kroniskSyktBarn

        assertEquals(innvilgedeVedtakKroniskSyktBarn.size, 5)
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2020-05-05/2021-02-14"), kildeId = "fra-infotrygd", barnIdentitetsnummer = "22222222222")   // Infotrygd frem til behandling1 i k9
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2021-02-15/2032-12-31"), kildeId = "$behandlingId1", barnIdentitetsnummer = "22222222222")  // Behandlingen1 i k9
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2033-01-01/2033-04-30"), kildeId = "$behandlingId2", barnIdentitetsnummer = "22222222222")  // Deler av behandling2 i k9 som ikke er avslått i behandling3
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2033-07-02/2033-12-31"), kildeId = "fra-infotrygd", barnIdentitetsnummer = "22222222222")   // Infotrygd etter behandling 3
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2020-05-05/2033-12-31"), kildeId = "fra-infotrygd", barnIdentitetsnummer = "33333333333")   // Infotrygd hele vedtak for barn nummer 2
    }

    private fun List<KroniskSyktBarnInnvilgetVedtak>.assertInneholder(periode: Periode, kildeId: String, barnIdentitetsnummer: String) {
        assertNotNull(firstOrNull { it.periode == periode && it.kilder.first().id == kildeId && it.barn.identitetsnummer == barnIdentitetsnummer })
    }

    private fun hentInnvilgedeVedtak(identitetsnummer: Identitetsnummer, periode: Periode) = runBlocking {
        applicationContext.innvilgedeVedtakService.hentInnvilgedeVedtak(identitetsnummer, periode, CorrelationId.genererCorrelationId())
    }

    private companion object {
        val Identitetsnummer1 = "11111111111".somIdentitetsnummer()
        val Identitetsnummer2 = "22222222222".somIdentitetsnummer()
        val OmsorgspengerSaksnummer1 = Identitetsnummer1.somMocketOmsorgspengerSaksnummer()
        val OmsorgspengerSaksnummer2 = Identitetsnummer2.somMocketOmsorgspengerSaksnummer()
    }
}