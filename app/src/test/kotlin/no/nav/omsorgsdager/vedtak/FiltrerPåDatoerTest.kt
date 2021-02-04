package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.periode
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.filtrerPåDatoer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

internal class FiltrerPåDatoerTest {
    @Test
    fun `Hverken fom eller tom satt`() {
        assertThat(vedtakene.filtrerPåDatoer(fom = null, tom = null)).hasSameElementsAs(vedtakene)
    }

    @Test
    fun `Filtrere kun med fom eller tom`() {
        assertFomEllerTom(dato = "2019-12-31".dato(), forventetResultat = emptySet())
        assertFomEllerTom(dato = "2020-04-30".dato(), forventetResultat = setOf(vedtak1))
        assertFomEllerTom(dato = "2020-11-01".dato(), forventetResultat = vedtakene)
        assertFomEllerTom(dato = "2021-01-15".dato(), forventetResultat = setOf(vedtak2, vedtak3))
        assertFomEllerTom(dato = "2022-02-15".dato(), forventetResultat = setOf(vedtak3))
        assertFomEllerTom(dato = "2022-02-16".dato(), forventetResultat = emptySet())
    }

    @Test
    fun `Filtrer med periode`() {
        assertPeriode(periode = "1999-01-01/2019-12-31".periode(), forventetResultat = emptySet())
        assertPeriode(periode = "2020-12-31/2022-01-01".periode(), forventetResultat = vedtakene)
        assertPeriode(periode = "2021-01-15/2030-01-01".periode(), forventetResultat = setOf(vedtak2, vedtak3))
        assertPeriode(periode = "2022-02-16/2030-12-31".periode(), forventetResultat = emptySet())
    }

    private fun assertFomEllerTom(dato: LocalDate, forventetResultat: Collection<GjeldendeVedtakTest.Companion.TestVedtak>) {
        assertThat(vedtakene.filtrerPåDatoer(fom = dato, tom = null)).hasSameElementsAs(forventetResultat)
        assertThat(vedtakene.filtrerPåDatoer(fom = null, tom = dato)).hasSameElementsAs(forventetResultat)
    }

    private fun assertPeriode(periode: Periode, forventetResultat: Collection<GjeldendeVedtakTest.Companion.TestVedtak>) {
        assertThat(vedtakene.filtrerPåDatoer(fom = periode.fom, tom = periode.tom)).hasSameElementsAs(forventetResultat)
    }


    private companion object {
        private val vedtak1 = "2020-01-01/2020-12-31".periode().vedtak()
        private val vedtak2 = "2020-05-01/2021-01-15".periode().vedtak()
        private val vedtak3 = "2020-11-01/2022-02-15".periode().vedtak()
        private val vedtakene = listOf(vedtak1, vedtak2, vedtak3)

        private fun Periode.vedtak() = GjeldendeVedtakTest.Companion.TestVedtak(
            status = VedtakStatus.INNVILGET,
            periode = this,
            statusSistEndret = ZonedDateTime.now(),
            barn = 1
        )
    }
}