package no.nav.omsorgsdager.vedtak

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

    private companion object {
        private data class TestVedtak(
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