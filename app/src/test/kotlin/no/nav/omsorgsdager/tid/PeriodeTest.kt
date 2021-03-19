package no.nav.omsorgsdager.tid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PeriodeTest {

    @Test
    fun `antall dager i perioden`() {
        assertEquals(1, Periode(enkeltdag = LocalDate.now()).antallDager)
        assertEquals(31, Periode("2020-01-01/2020-01-31").antallDager)
        assertEquals(365, Periode(2021).antallDager)
        assertEquals(2914635, Periode("2020-01-01/9999-12-31").antallDager)
    }

    @Test
    fun `sanitized periode`() {
        val okPeriode = Periode("2020-01-01/2040-12-31")
        assertEquals(okPeriode.sanitized(), okPeriode)
        val enDagForLangPeriode = Periode("2020-01-01/2041-01-01")
        assertEquals(enDagForLangPeriode.sanitized(), okPeriode)
        val langtFremITidPeriode = Periode("2030-05-05/2048-12-31")
        assertEquals(langtFremITidPeriode.sanitized(), langtFremITidPeriode)
    }
}