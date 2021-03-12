import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Tidslinje
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
            Periode("2022-03-01/2022-12-31"),
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

}