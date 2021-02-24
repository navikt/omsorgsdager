package no.nav.omsorgsdager.vedtak

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.omsorgsdager.*
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.behandling.NyBehandling
import no.nav.omsorgsdager.parter.Barn
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSakGatway
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.Kilde
import no.nav.omsorgsdager.vedtak.dto.KroniskSyktBarnInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.infotrygd.KroniskSyktBarnInfotrygdInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(ApplicationContextExtension::class)
internal class InnvilgedeVedtakServiceTest(
    applicationContextBuilder: ApplicationContext.Builder) {

    private val mockedOmsorgspengerSakGateway = mockk<OmsorgspengerSakGatway>().also {
        coEvery { it.hentSaksnummer(Identitetsnummer1, any()) }.returns(OmsorgspengerSaksnummer1)
    }

    private val mockedOmsorgspengerInfotrygdRammevedtakGateway = mockk<OmsorgspengerInfotrygdRammevedtakGateway>().also {
        val kroniskSyktBarnInfotrygdVedtak = KroniskSyktBarnInfotrygdInnvilgetVedtak(
            vedtatt = LocalDate.parse("2020-05-05"),
            gyldigFraOgMed = LocalDate.parse("2020-05-05"),
            gyldigTilOgMed = LocalDate.parse("2033-12-31"),
            kilder = setOf(Kilde(id = "fra-infotrygd", type = "Infotrygd")),
            barnetsFødselsdato = LocalDate.parse("2020-01-01"),
            barnetsIdentitetsnummer = "22222222222".somIdentitetsnummer()
        )
        coEvery { it.hentInnvilgedeVedtak(Identitetsnummer1, any(), any()) }.returns(listOf(
            kroniskSyktBarnInfotrygdVedtak, kroniskSyktBarnInfotrygdVedtak.copy(barnetsIdentitetsnummer = "33333333333".somIdentitetsnummer())
        ))
    }

    private val applicationContext = applicationContextBuilder.also {
        it.omsorgspengerSakGatway = mockedOmsorgspengerSakGateway
        it.omsorgspengerInfotrygdRammevedtakGateway = mockedOmsorgspengerInfotrygdRammevedtakGateway
    }.buildStarted()

    @Test
    fun `Kombinerer innvilgede vedtak om kronisk sykt barn fra Infotrygd & K9-sak`() {
        val behandlingId = "${UUID.randomUUID()}".somK9BehandlingId()

        applicationContext.behandlingService.lagre(
            behandling = NyBehandling(
                saksnummer = "1234".somK9Saksnummer(),
                behandlingId = behandlingId,
                tidspunkt = ZonedDateTime.now(),
                type = BehandlingType.KRONISK_SYKT_BARN,
                status = BehandlingStatus.INNVILGET,
                periode = Periode("2021-02-15/2032-12-31"),
                grunnlag = Json.tomJson()
            ),
            parter = listOf(
                Søker(identitetsnummer = Identitetsnummer1, omsorgspengerSaksnummer = OmsorgspengerSaksnummer1),
                Barn(fødselsdato = LocalDate.parse("2020-01-01"), identitetsnummer = "22222222222".somIdentitetsnummer())
            )
        )
        val innvilgedeVedtakKroniskSyktBarn = hentInnvilgedeVedtak("11111111111".somIdentitetsnummer(), Periode("2020-01-01/2050-01-01")).kroniskSyktBarn
        assertEquals(innvilgedeVedtakKroniskSyktBarn.size, 4)
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2020-05-05/2021-02-14"), kildeId = "fra-infotrygd", barnIdentitetsnummer = "22222222222")   // Infotrygd frem til behandling i k9
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2021-02-15/2032-12-31"), kildeId = "$behandlingId", barnIdentitetsnummer = "22222222222")   // Behandlingen i k9
        innvilgedeVedtakKroniskSyktBarn.assertInneholder(periode = Periode("2033-01-01/2033-12-31"), kildeId = "fra-infotrygd", barnIdentitetsnummer = "22222222222")   // Infotrygd etter behndlingen i k9
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
        val OmsorgspengerSaksnummer1 = Identitetsnummer1.somMocketOmsorgspengerSaksnummer()
    }
}