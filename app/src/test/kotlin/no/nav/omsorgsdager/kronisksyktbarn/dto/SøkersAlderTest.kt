package no.nav.omsorgsdager.kronisksyktbarn.dto

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SøkersAlderTest {

    @Test
    fun `Parsing av søkers ålder`() {
        val søkerOver70År = Søker(
            identitetsnummer = "123",
            fødselsdato = LocalDate.now().minusYears(80)
        )

        Assertions.assertThat(søkerOver70År.sisteDagSøkerHarRettTilOmsorgsdager == LocalDate.now().minusYears(10).minusDays(1))
    }

}