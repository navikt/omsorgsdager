import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Tidslinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TidslinjeTest {

    @Test
    fun `Legge til perioder som ikke finnes i tidslinjen`() {
        val År1999 = Periode(2019)
        val Sommer2021 = Periode("2021-05-01/2021-09-01")
        val Juli2021 = Periode("2021-07-01/2021-07-31")
        val Febuar2022 = Periode("2022-02-01/2022-02-28")
        val År2022 = Periode(2022)

        val tidsserie = Tidslinje(listOf(Sommer2021, Febuar2022))
            .leggTil(Juli2021)
            .leggTil(År2022)
            .leggTil(År1999)

        assertThat(tidsserie.nyePerioder()).hasSameElementsAs(setOf(
            År1999,
            Periode("2022-01-01/2022-01-31"),
            Periode("2022-03-01/2022-12-31"),
        ))
    }
}