package no.nav.omsorgsdager.tid

import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class GjeldendeTest {

    @Test
    fun `tom liste`() {
        assertThat(listOf<TestGjeldende>().gjeldende()).isEmpty()
    }

    @Test
    fun `to forskjellige barn samme periode`() {
        val nå = ZonedDateTime.now()
        val periode = Periode("2021-01-01/2021-12-31")
        val gjeldende1 = TestGjeldende(
            tidspunkt = nå,
            barn = 1,
            periode = periode
        )
        val gjeldende2 = gjeldende1.copy(barn = 2)
        val gjeldende = listOf(gjeldende1, gjeldende2)
        assertThat(gjeldende.gjeldende()).hasSameElementsAs(gjeldende)

    }

    @Test
    fun `samme barn samme periode`() {
        val nå = ZonedDateTime.now()
        val periode = Periode("2021-01-01/2021-12-31")
        val gjeldende1 = TestGjeldende(
            tidspunkt = nå,
            barn = 1,
            periode = periode
        )
        val gjeldende2 = gjeldende1.copy(
            tidspunkt = nå.plusMinutes(5)
        )
        val gjeldende = listOf(gjeldende1, gjeldende2)
        assertThat(gjeldende.gjeldende()).hasSameElementsAs(setOf(gjeldende2))
    }

    @Test
    fun `overlapende gjeldende`() {
        val nå = ZonedDateTime.now()
        val gjeldende1 = TestGjeldende(
            tidspunkt = nå,
            barn = 1,
            periode = Periode("2021-01-01/2021-12-31")
        )
        val gjeldende2 = gjeldende1.copy(
            periode = Periode("2021-12-01/2021-12-31"),
            tidspunkt = nå.plusMinutes(5)
        )
        val gjeldende3 = gjeldende1.copy(
            periode = Periode("2021-12-31/2021-12-31"),
            tidspunkt = nå.plusMinutes(10)
        )

        val gjeldende = listOf(gjeldende1, gjeldende3, gjeldende2)
        assertThat(gjeldende.gjeldende()).hasSameElementsAs(setOf(
            gjeldende3,
            gjeldende2.copy(periode = Periode("2021-12-01/2021-12-30")),
            gjeldende1.copy(periode = Periode("2021-01-01/2021-11-30"))
        ))
    }

    internal companion object {
        internal data class TestGjeldende(
            override val tidspunkt: ZonedDateTime,
            internal val barn: Int,
            override val periode: Periode) : Gjeldende.KanUtledeGjeldende {
            override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
                periode = nyPeriode
            )
            override val enPer = barn
        }
    }
}