package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.aleneomsorg.AleneOmsorgBehandling
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.GjeldendeBehandlinger
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.testutils.mocketK9Saksnummer
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.periode
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakService.Companion.slåSammenMed
import no.nav.omsorgsdager.vedtak.dto.AleneOmsorgInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class SlåSammenAleneOmsorgTest {

    @Test
    fun `alene omsorg i Infotrygd, Rammemeldinger & K9`() {
        val barnetsIdentitetsnummer = "11111111111".somIdentitetsnummer()
        val søkersIdentitetsnummer = "22222222222".somIdentitetsnummer()
        val barnetsFødselsdato = "2015-05-05".dato()
        val nå = ZonedDateTime.now()


        val fraInfotrygd = AleneOmsorgInnvilgetVedtak(
            tidspunkt = nå,
            kilder = emptySet(),
            periode = "2016-05-06/2020-12-31".periode(),
            barn = Barn(identitetsnummer = null, fødselsdato = barnetsFødselsdato)
        )

        val fraRammemeldinger = AleneOmsorgInnvilgetVedtak(
            tidspunkt = nå.plusMinutes(1),
            kilder = setOf(),
            periode = "2019-02-15/2030-12-31".periode(),
            barn = Barn(identitetsnummer = barnetsIdentitetsnummer, fødselsdato = barnetsFødselsdato)
        )

        val slåttSammenUtenBehandlingerIK9 = InnvilgedeVedtak(
            aleneOmsorg = listOf(fraInfotrygd, fraRammemeldinger)
        ).slåSammenMed(GjeldendeBehandlinger())

        assertThat(slåttSammenUtenBehandlingerIK9.aleneOmsorg).hasSameElementsAs(setOf(
            fraInfotrygd.copy(barn = fraRammemeldinger.barn, enPer = barnetsFødselsdato, periode = "2016-05-06/2019-02-14".periode()), // Frem til rammemelding
            fraRammemeldinger.copy(enPer = barnetsFødselsdato)
        ))

        val søker = Søker(
            identitetsnummer = søkersIdentitetsnummer,
            omsorgspengerSaksnummer = søkersIdentitetsnummer.somMocketOmsorgspengerSaksnummer(),
            aktørId = "55555".somAktørId()
        )

        val innvilgetBehandling = AleneOmsorgBehandling(
            k9Saksnummer = mocketK9Saksnummer(),
            k9behandlingId = K9BehandlingId.generateK9BehandlingId(),
            tidspunkt = nå.plusMinutes(2),
            periode = Periode("2022-12-01/2025-05-05"),
            søker = søker,
            barn = no.nav.omsorgsdager.parter.Barn(
                identitetsnummer = barnetsIdentitetsnummer,
                fødselsdato = barnetsFødselsdato,
                omsorgspengerSaksnummer = barnetsIdentitetsnummer.somMocketOmsorgspengerSaksnummer(),
                aktørId = "66666".somAktørId()
            ),
            status = BehandlingStatus.INNVILGET
        )

        val slåttSammenMedInnvilgelseIK9 = InnvilgedeVedtak(
            aleneOmsorg = listOf(fraInfotrygd, fraRammemeldinger)
        ).slåSammenMed(GjeldendeBehandlinger(
            alleAleneOmsorg = listOf(innvilgetBehandling)
        ))

        val fraInnvilgetBehandling = slåttSammenMedInnvilgelseIK9.aleneOmsorg.firstOrNull { it.tidspunkt == innvilgetBehandling.tidspunkt }!!

        assertThat(slåttSammenMedInnvilgelseIK9.aleneOmsorg).hasSameElementsAs(setOf(
            fraInfotrygd.copy(barn = fraInnvilgetBehandling.barn, enPer = barnetsFødselsdato, periode = "2016-05-06/2019-02-14".periode()), // Frem til rammemelding
            fraRammemeldinger.copy(barn = fraInnvilgetBehandling.barn, enPer = barnetsFødselsdato, periode = "2019-02-15/2022-11-30".periode()), // Frem til innvilget behandling
            fraInnvilgetBehandling.also { require(it.periode == Periode("2022-12-01/2025-05-05")) }, // Innvilget behandling
            fraRammemeldinger.copy(barn = fraInnvilgetBehandling.barn, enPer = barnetsFødselsdato, periode = "2025-05-06/2030-12-31".periode()) // Etter innvilget behandling fra rammemelding
        ))

        val avslåttBehandling = innvilgetBehandling.copy(
            status = BehandlingStatus.AVSLÅTT,
            periode = "2023-05-17/9999-12-31".periode(),
            tidspunkt = nå.plusMinutes(3),
            k9behandlingId = K9BehandlingId.generateK9BehandlingId()
        )

        val slåttSammenMedBeggeIK9 = InnvilgedeVedtak(
            aleneOmsorg = listOf(fraInfotrygd, fraRammemeldinger)
        ).slåSammenMed(GjeldendeBehandlinger(
            alleAleneOmsorg = listOf(innvilgetBehandling, avslåttBehandling)
        ))

        assertThat(slåttSammenMedBeggeIK9.aleneOmsorg).hasSameElementsAs(setOf(
            fraInfotrygd.copy(barn = fraInnvilgetBehandling.barn, enPer = barnetsFødselsdato, periode = "2016-05-06/2019-02-14".periode()), // Frem til rammemelding
            fraRammemeldinger.copy(barn = fraInnvilgetBehandling.barn, enPer = barnetsFødselsdato, periode = "2019-02-15/2022-11-30".periode()), // Frem til innvilget behandling
            fraInnvilgetBehandling.copy(periode = "2022-12-01/2023-05-16".periode()), // Innvilget behandling frem til avslått
        ))
    }
}