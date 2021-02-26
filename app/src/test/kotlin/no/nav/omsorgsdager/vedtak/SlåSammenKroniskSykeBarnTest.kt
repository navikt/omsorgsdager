package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.GjeldendeBehandlinger
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnBehandling
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.testutils.mocketK9Saksnummer
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakService.Companion.slåSammenMed
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak
import no.nav.omsorgsdager.vedtak.dto.Kilde
import no.nav.omsorgsdager.vedtak.dto.Kilde.Companion.somKilder
import no.nav.omsorgsdager.vedtak.dto.KroniskSyktBarnInnvilgetVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

import no.nav.omsorgsdager.parter.Barn as BarnPart

internal class SlåSammenKroniskSykeBarnTest {

    @Test
    fun `Ingen vedtak i Infotrygd og ingen behandlinger i K9-sak`() {
        val ingenVedtak = InnvilgedeVedtak()
        assertEquals(ingenVedtak, ingenVedtak.slåSammenMed(GjeldendeBehandlinger()))
    }

    @Test
    fun `Har kun vedtak i Infotrygd`() {
        val tvilling1 = KroniskSyktBarnInnvilgetVedtak(
            barn = Barn("29099011111".somIdentitetsnummer(), fødselsdato = "1990-09-20".dato()),
            periode = Periode(2020),
            tidspunkt = ZonedDateTime.now(),
            kilder = emptySet()
        )
        val tvilling2 = tvilling1.copy(
            barn = Barn("29099011112".somIdentitetsnummer(), fødselsdato = "1990-09-20".dato()),
            tidspunkt = ZonedDateTime.now().plusMinutes(1)
        )
        var slåttSammen = InnvilgedeVedtak(
            kroniskSyktBarn = listOf(tvilling1, tvilling2)
        ).slåSammenMed(GjeldendeBehandlinger())

        // Har unke FNR så får 2 vedtak for samme periode
        assertThat(slåttSammen.kroniskSyktBarn).hasSize(2)

        // Den med seneste tidspunkt har DNR (matches da på fødselsdato ettersom vi ikke klarer å matche på noe annet)
        // Klarer da ikke å se at det er tvillinger..
        slåttSammen = InnvilgedeVedtak(
            kroniskSyktBarn = listOf(tvilling1, tvilling2.copy(barn = tvilling2.barn.copy(identitetsnummer = "49099011112")))
        ).slåSammenMed(GjeldendeBehandlinger())

        assertThat(slåttSammen.kroniskSyktBarn).hasSize(1)
        assertThat(slåttSammen.kroniskSyktBarn.first().barn.identitetsnummer).isSameAs("49099011112")
    }

    @Test
    fun `Har kun behandlinger i K9-sak`() {
        val identitetsnummer1 = "11111111111".somIdentitetsnummer()
        val identitetsnummer2 = "11111111112".somIdentitetsnummer()
        val identitetsnummer3 = "11111111113".somIdentitetsnummer()
        val identitetsnummer4 = "11111111114".somIdentitetsnummer()

        val k9Saksnummer = mocketK9Saksnummer()
        val barnetsOmsorgspengerSaksnummer = identitetsnummer2.somMocketOmsorgspengerSaksnummer()

        val søker = Søker(identitetsnummer = identitetsnummer1, omsorgspengerSaksnummer = identitetsnummer1.somMocketOmsorgspengerSaksnummer())

        val tidspunkt = ZonedDateTime.now()
        val fødselsdato1 = LocalDate.now()

        val behandlingId1 = K9BehandlingId.generateK9BehandlingId()
        val behandling1 = KroniskSyktBarnBehandling(
            k9Saksnummer = k9Saksnummer,
            k9behandlingId = behandlingId1,
            tidspunkt = tidspunkt,
            periode = Periode(2021),
            søker = søker,
            barn = BarnPart(identitetsnummer = identitetsnummer2, omsorgspengerSaksnummer = barnetsOmsorgspengerSaksnummer, fødselsdato = fødselsdato1),
            status = BehandlingStatus.INNVILGET
        )

        val behandlingId2 = K9BehandlingId.generateK9BehandlingId()
        val sisteInnvilgetFødselsdato = fødselsdato1.plusDays(1)
        val behandling2 = KroniskSyktBarnBehandling(
            k9Saksnummer = k9Saksnummer,
            k9behandlingId = behandlingId2,
            tidspunkt = tidspunkt.plusMinutes(1),
            periode = Periode("2021-05-02/2021-12-31"),
            søker = søker,
            barn = BarnPart(identitetsnummer = identitetsnummer3, omsorgspengerSaksnummer = barnetsOmsorgspengerSaksnummer, fødselsdato = sisteInnvilgetFødselsdato),
            status = BehandlingStatus.INNVILGET
        )

        val behandlingId3 = K9BehandlingId.generateK9BehandlingId()
        val behandling3Barn = BarnPart(identitetsnummer = identitetsnummer4, omsorgspengerSaksnummer = barnetsOmsorgspengerSaksnummer, fødselsdato = fødselsdato1.plusDays(2))
        val behandling3 = KroniskSyktBarnBehandling(
            k9Saksnummer = k9Saksnummer,
            k9behandlingId = behandlingId3,
            tidspunkt = tidspunkt.plusMinutes(2),
            periode = Periode("2021-12-01/2021-12-31"),
            søker = søker,
            barn = behandling3Barn,
            status = BehandlingStatus.AVSLÅTT
        )

        val slåttSammen = InnvilgedeVedtak().slåSammenMed(GjeldendeBehandlinger(
            alleKroniskSyktBarn = listOf(behandling1, behandling2, behandling3)
        ))

        // Barnet slik det var på siste behandling, tross at den er avslått
        val vedtakBarn = Barn(identitetsnummer = behandling3Barn.identitetsnummer, fødselsdato = behandling3Barn.fødselsdato, omsorgspengerSaksnummer = behandling3Barn.omsorgspengerSaksnummer)

        assertThat(slåttSammen.kroniskSyktBarn).hasSameElementsAs(listOf(
            KroniskSyktBarnInnvilgetVedtak(
                barn = vedtakBarn,
                kilder = behandlingId1.somKilder(),
                periode = Periode("2021-01-01/2021-05-01"),
                tidspunkt = behandling1.tidspunkt,
                enPer = "${behandling3Barn.omsorgspengerSaksnummer}"
            ),
            KroniskSyktBarnInnvilgetVedtak(
                barn = vedtakBarn,
                kilder = behandlingId2.somKilder(),
                periode = Periode("2021-05-02/2021-11-30"),
                tidspunkt = behandling2.tidspunkt,
                enPer = "${behandling3Barn.omsorgspengerSaksnummer}"
            )
        ))

    }

    @Test
    fun `Har kun vedtak i Infotrygd, avslått behandling i K9-sak`() {
        val barnIdentitetsnummer = "29099011111".somIdentitetsnummer()
        val barnFødselsdato = "1990-09-20".dato()
        val infotrygdKilder = setOf(Kilde(id = "1", type = "Infotrygd"))
        val infotrygdTidspunkt = ZonedDateTime.now()

        val infotrygd = KroniskSyktBarnInnvilgetVedtak(
            barn = Barn(identitetsnummer = barnIdentitetsnummer, fødselsdato = barnFødselsdato),
            periode = Periode(2021),
            tidspunkt = infotrygdTidspunkt,
            kilder = infotrygdKilder
        )

        val søkerIdentitetsnummer = "11111111111".somIdentitetsnummer()
        val søker = Søker(identitetsnummer = søkerIdentitetsnummer, omsorgspengerSaksnummer = søkerIdentitetsnummer.somMocketOmsorgspengerSaksnummer())
        val behandlingId = K9BehandlingId.generateK9BehandlingId()

        val avslåttBehandling = KroniskSyktBarnBehandling(
            k9Saksnummer = mocketK9Saksnummer(),
            k9behandlingId = behandlingId,
            tidspunkt = infotrygdTidspunkt.plusMinutes(1),
            periode = Periode("2021-12-01/2021-12-31"),
            søker = søker,
            barn = BarnPart(identitetsnummer = barnIdentitetsnummer, fødselsdato = barnFødselsdato, omsorgspengerSaksnummer = barnIdentitetsnummer.somMocketOmsorgspengerSaksnummer()),
            status = BehandlingStatus.AVSLÅTT
        )

        val slåttSammen = InnvilgedeVedtak(kroniskSyktBarn = listOf(infotrygd)).slåSammenMed(GjeldendeBehandlinger(
            alleKroniskSyktBarn = listOf(avslåttBehandling)
        ))

        assertThat(slåttSammen.kroniskSyktBarn).hasSameElementsAs(listOf(
            KroniskSyktBarnInnvilgetVedtak(
                barn = Barn(identitetsnummer = barnIdentitetsnummer, fødselsdato = barnFødselsdato, omsorgspengerSaksnummer = barnIdentitetsnummer.somMocketOmsorgspengerSaksnummer()),
                kilder = infotrygdKilder,
                periode = Periode("2021-01-01/2021-11-30"),
                tidspunkt = infotrygdTidspunkt,
                enPer = "$barnIdentitetsnummer"
            )
        ))

    }

}