package no.nav.omsorgsdager.tid

import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
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

    @Test
    fun `revurdering innvilgede perioder`() {
        val tidspunkt = ZonedDateTime.now()
        val barnetFødt = "2012-05-05".dato()
        val barnetFyller18 = barnetFødt.sisteDagIÅretOm18År()

        val fraInfotrygd = TestGjeldende(
            tidspunkt = tidspunkt,
            barn = 1,
            periode = Periode(fom = barnetFødt, tom = barnetFyller18)
        )

        val behandling1 = fraInfotrygd.copy(
            tidspunkt = tidspunkt.plusDays(1),
            periode = Periode(fom = "2021-04-01".dato(), tom = barnetFyller18)
        )

        val behandling2 = fraInfotrygd.copy(
            tidspunkt = tidspunkt.plusDays(2),
            periode = Periode("2021-05-06".dato(), tom = barnetFyller18)
        )

        val gjeldende = listOf(fraInfotrygd, behandling1, behandling2)
        assertThat(gjeldende.gjeldende()).hasSameElementsAs(setOf(
            behandling2,
            behandling1.copy(periode = Periode(fom = "2021-04-01".dato(), tom = "2021-05-05".dato())),
            fraInfotrygd.copy(periode = Periode(fom = barnetFødt, tom = "2021-03-31".dato()))
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