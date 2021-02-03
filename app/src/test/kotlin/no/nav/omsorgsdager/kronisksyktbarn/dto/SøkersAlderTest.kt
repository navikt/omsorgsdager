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

        Assertions.assertThat(søkerOver70År.erOver70EtterDato(LocalDate.now()))
        Assertions.assertThat(!søkerOver70År.erOver70EtterDato(LocalDate.now().minusYears(50)))

        Assertions.assertThat(søkerOver70År.ålderVidDato(LocalDate.now()) == 80)
        Assertions.assertThat(søkerOver70År.ålderVidDato(LocalDate.now().plusYears(5)) == 85)
    }

}