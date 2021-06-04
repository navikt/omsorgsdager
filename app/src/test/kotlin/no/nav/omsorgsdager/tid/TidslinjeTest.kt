package no.nav.omsorgsdager.tid

import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TidslinjeTest {

    @Test
    fun `Legge til perioder som ikke finnes i tidslinjen`() {
        val År1999 = Periode(2019)
        val Sommer2021 = Periode("2021-05-01/2021-09-01")
        val Juli2021 = Periode("2021-07-01/2021-07-31")
        val Febuar2022 = Periode("2022-02-01/2022-02-28")
        val År2022 = Periode(2022)

        val tidslinje = Tidslinje(listOf(Sommer2021, Febuar2022))
            .leggTil(Juli2021)
            .leggTil(År2022)
            .leggTil(År1999)

        assertThat(tidslinje.nyePerioder()).hasSameElementsAs(setOf(
            År1999,
            Periode("2022-01-01/2022-01-31"),
            Periode("2022-03-01/2022-12-31")
        ))
    }

    @Test
    fun `Enkeltdager i periode`() {
        val JanTilApr2021 = Periode("2021-01-01/2021-04-30")
        val NovTilDes2021 = Periode("2021-11-01/2021-12-31")
        val tidslinje = Tidslinje(listOf(JanTilApr2021, NovTilDes2021))
            .leggTil(Periode(LocalDate.parse("2020-12-30")))
            .leggTil(Periode(LocalDate.parse("2021-04-30"))) // Allerede med
            .leggTil(Periode(LocalDate.parse("2021-06-01")))
            .leggTil(Periode(LocalDate.parse("2021-12-31"))) // Allerede med
            .leggTil(Periode(LocalDate.parse("2022-01-02")))

        assertThat(tidslinje.nyePerioder()).hasSameElementsAs(setOf(
            Periode(LocalDate.parse("2020-12-30")),
            Periode(LocalDate.parse("2021-06-01")),
            Periode(LocalDate.parse("2022-01-02"))
        ))
    }

    @Test
    fun `Legge til periode med tidenes ende`() {
        val År2021 = Periode(2021)
        val EvigTid = Periode("2020-06-01/9999-12-31")
        val tidslinje = Tidslinje(listOf(År2021))
            .leggTil(EvigTid)

        assertThat(tidslinje.nyePerioder()).hasSameElementsAs(setOf(
            Periode("2020-06-01/2020-12-31"),
            Periode("2022-01-01/9999-12-31")
        ))
    }

    @Test
    fun `Utgangspunkt i tidenes ende`() {
        val År1999TilTideneseEnde = Periode("1999-06-02/9999-12-31")
        val År2020TilTidenesEnde = Periode("2020-06-01/9999-12-31")
        val tidslinje = Tidslinje(listOf(År2020TilTidenesEnde))
            .leggTil(År1999TilTideneseEnde)

        assertThat(tidslinje.nyePerioder()).hasSameElementsAs(setOf(
            Periode("1999-06-02/2020-05-31")
        ))
    }

    @Test
    fun `Mange perioder til tidenes ende`() {
        val periode0 = "2021-01-10/2021-03-15".periode()
        val periode1 = "2022-04-12/9999-12-31".periode()
        val periode2 = "2025-04-12/9999-12-31".periode()
        val periode3 = "1999-07-02/9999-12-31".periode()

        val tidslinje = Tidslinje(listOf(periode0))
            .leggTil(periode1)
            .leggTil(periode2)
            .leggTil(periode3)

        assertThat(tidslinje.nyePerioder()).hasSameElementsAs(setOf(
            "1999-07-02/2021-01-09".periode(),
            "2021-03-16/9999-12-31".periode()
        ))
    }

    @Test
    fun `Perioder som til slutt blir en lang periode`() {
        val periode0 = Periode("2021-03-03".dato())
        val periode1 = Periode("2021-03-01".dato())
        val periode2 = Periode("2021-02-03/2021-02-27")
        val periode3 = Periode("2021-03-02".dato())
        val periode4 = Periode("2021-02-28".dato())

        val tidslinje = Tidslinje(emptyList())
        assertThat(tidslinje.nyePerioder()).isEmpty()
        assertThat(tidslinje.leggTil(periode0).nyePerioder()).hasSameElementsAs(setOf(
            "2021-03-03/2021-03-03".periode()
        ))
        assertThat(tidslinje.leggTil(periode1).nyePerioder()).hasSameElementsAs(setOf(
            "2021-03-03/2021-03-03".periode(),
            "2021-03-01/2021-03-01".periode()
        ))
        assertThat(tidslinje.leggTil(periode2).nyePerioder()).hasSameElementsAs(setOf(
            "2021-03-03/2021-03-03".periode(),
            "2021-03-01/2021-03-01".periode(),
            "2021-02-03/2021-02-27".periode()
        ))
        assertThat(tidslinje.leggTil(periode3).nyePerioder()).hasSameElementsAs(setOf(
            "2021-03-01/2021-03-03".periode(),
            "2021-02-03/2021-02-27".periode()
        ))
        assertThat(tidslinje.leggTil(periode4).nyePerioder()).hasSameElementsAs(setOf(
            "2021-02-03/2021-03-03".periode()
        ))
    }
}