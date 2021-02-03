package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.gjeldendeVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class GjeldendeVedtakTest {

    @Test
    fun `ingen vedtak`() {
        assertThat(listOf<TestVedtak>().gjeldendeVedtak()).isEmpty()
    }

    @Test
    fun `kun vedtaksforslag`() {
        val nå = ZonedDateTime.now()
        assertThat(listOf(
            TestVedtak(
                status = VedtakStatus.FORSLAG,
                statusSistEndret = nå,
                barn = "2",
                periode = Periode("2021-01-01/2021-12-31")
            ),
            TestVedtak(
                status = VedtakStatus.FORSLAG,
                statusSistEndret = nå,
                barn = "2",
                periode = Periode("2021-01-01/2021-12-31")
            ),
        ).gjeldendeVedtak()).isEmpty()
    }

    @Test
    fun `to forskjellige barn samme periode`() {
        val nå = ZonedDateTime.now()
        val periode = Periode("2021-01-01/2021-12-31")
        val vedtakBarn1 = TestVedtak(
            status = VedtakStatus.FASTSATT,
            statusSistEndret = nå,
            barn = "1",
            periode = periode
        )
        val vedtakBarn2 = vedtakBarn1.copy(barn = "2")
        val vedtak = listOf(vedtakBarn1, vedtakBarn2)
        assertThat(vedtak.gjeldendeVedtak()).hasSameElementsAs(vedtak)

    }

    @Test
    fun `samme barn samme periode`() {
        val nå = ZonedDateTime.now()
        val periode = Periode("2021-01-01/2021-12-31")
        val vedtak1 = TestVedtak(
            status = VedtakStatus.DEAKTIVERT,
            statusSistEndret = nå,
            barn = "1",
            periode = periode
        )
        val vedtak2 = vedtak1.copy(
            status = VedtakStatus.FASTSATT,
            statusSistEndret = nå.plusMinutes(5)
        )
        val vedtak = listOf(vedtak1, vedtak2)
        assertThat(vedtak.gjeldendeVedtak()).hasSameElementsAs(setOf(vedtak2))
    }

    @Test
    fun `overlapende vedtak`() {
        val nå = ZonedDateTime.now()
        val vedtak1 = TestVedtak(
            status = VedtakStatus.DEAKTIVERT,
            statusSistEndret = nå,
            barn = "1",
            periode = Periode("2021-01-01/2021-12-31")
        )
        val vedtak2 = vedtak1.copy(
            periode = Periode("2021-12-01/2021-12-31"),
            status = VedtakStatus.FASTSATT,
            statusSistEndret = nå.plusMinutes(5)
        )
        val vedtak3 = vedtak1.copy(
            periode = Periode("2021-12-01/2021-12-31"),
            status = VedtakStatus.FORSLAG,
            statusSistEndret = nå.plusMinutes(10)
        )

        val vedtak = listOf(vedtak1, vedtak2, vedtak3)
        assertThat(vedtak.gjeldendeVedtak()).hasSameElementsAs(setOf(
            vedtak2,
            vedtak1.copy(periode = Periode("2021-01-01/2021-11-30"))
        ))
    }

    @Test
    fun `vedtak som har blitt revurdert flere ganger` () {
        val nå = ZonedDateTime.now()
        val periodeBarn1 = Periode("2021-01-01/2030-12-31")
        val periodeBarn2 = Periode("2021-01-05/2035-12-31")

        // Avslag førstegangssøknad & klage, fastsatt i klageinstans
        val vedtak1Barn1 = TestVedtak(status = VedtakStatus.DEAKTIVERT, statusSistEndret = nå, barn = 1, periode = periodeBarn1)
        val vedtak2Barn1 = TestVedtak(status = VedtakStatus.DEAKTIVERT, statusSistEndret = nå.plusMinutes(1), barn = 1, periode = periodeBarn1)
        val vedtak3Barn1 = TestVedtak(status = VedtakStatus.FASTSATT, statusSistEndret = nå.plusMinutes(2), barn = 1, periode = periodeBarn1)

        // Avslag førstegangssøknad, fastsatt i klage, åpnet ny behandling
        val vedtak1Barn2 = TestVedtak(status = VedtakStatus.DEAKTIVERT, statusSistEndret = nå, barn = 2, periode = periodeBarn2)
        val vedtak2Barn2 = TestVedtak(status = VedtakStatus.FASTSATT, statusSistEndret = nå.plusMinutes(1), barn = 2, periode = periodeBarn2)
        val vedtak3Barn2 = TestVedtak(status = VedtakStatus.FORSLAG, statusSistEndret = nå.plusMinutes(2), barn = 2, periode = periodeBarn2)

        // Legge til i tilfeldig rekkefølge
        val vedtak = listOf(vedtak3Barn1, vedtak3Barn2, vedtak1Barn1, vedtak1Barn2, vedtak2Barn2, vedtak2Barn1)

        assertThat(vedtak.gjeldendeVedtak()).hasSameElementsAs(setOf(
            vedtak3Barn1,
            vedtak2Barn2
        ))
    }

    @Test
    fun `revurderinger på kortere perioder enn de første vedtakene`() {
        val nå = ZonedDateTime.now()
        val periode1Barn1 = Periode("2021-01-01/2030-12-31")
        val periode2Barn1 = Periode("2021-04-01/2030-11-30")

        val periode1Barn2 = Periode("2021-01-05/2035-12-31")
        val periode2Barn2 = Periode("2021-06-05/2035-12-31")

        val vedtak1Barn1 = TestVedtak(status = VedtakStatus.FASTSATT, statusSistEndret = nå, barn = 1, periode = periode1Barn1, behandlingId = "1")
        val vedtak2Barn1 = TestVedtak(status = VedtakStatus.FASTSATT, statusSistEndret = nå.plusMinutes(1), barn = 1, periode = periode2Barn1, behandlingId = "2")

        val vedtak1Barn2 = TestVedtak(status = VedtakStatus.FASTSATT, statusSistEndret = nå, barn = 2, periode = periode1Barn2, behandlingId = "3")
        val vedtak2Barn2 = TestVedtak(status = VedtakStatus.FASTSATT, statusSistEndret = nå.plusMinutes(1), barn = 2, periode = periode2Barn2, behandlingId = "4")

        val vedtak = listOf(vedtak1Barn1, vedtak1Barn2, vedtak2Barn2, vedtak2Barn1)

        assertThat(vedtak.gjeldendeVedtak()).hasSameElementsAs(setOf(
            // Barn 1
            vedtak1Barn1.kopiMedNyPeriode(Periode("2021-01-01/2021-03-31")), // Vedtak 1 til vedtak 2 begynner
            vedtak2Barn1.kopiMedNyPeriode(Periode("2021-04-01/2030-11-30")), // Vedtak 2 i sin helhet
            vedtak1Barn1.kopiMedNyPeriode(Periode("2030-12-01/2030-12-31")), // Vedtak 1 etter vedtak 2 slutter
            // Barn 2
            vedtak1Barn2.kopiMedNyPeriode(Periode("2021-01-05/2021-06-04")), // Vedtak 1 frem til vedtak 2 begynner
            vedtak2Barn2                                                         // Vedtak 2 i sin helhet
        ))
    }

    internal companion object {
        internal data class TestVedtak(
            override val saksnummer: Saksnummer = "1",
            override val behandlingId: BehandlingId = "1",
            override val status: VedtakStatus,
            override val statusSistEndret: ZonedDateTime,
            override val barn: Any,
            override val periode: Periode) : Vedtak {
            override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
                periode = nyPeriode
            )
        }
    }
}