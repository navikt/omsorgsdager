package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SøkerOver70ÅrTest(
    private val testApplicationEngine: TestApplicationEngine) {

    @Test
    @Order(1)
    fun `Sende inn søknad der søker fyller 70 år 2025, barnet fyller 18 år 2038`() {

        @Language("JSON")
        val request = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "mottatt": "2020-12-31T23:59:59.000Z",
                "søker": {
                    "identitetsnummer": "123",
                    "fødselsdato": "1955-01-01"
                },
                "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "2020-01-01"
                }
            }
        """.trimIndent()

        @Language("JSON")
        val forventetResponse = """
        {
            "status": "FORSLAG",
            "uløsteAksjonspunkter": {
                "LEGEERKLÆRING": {}
            }
        }""".trimIndent()

        with(testApplicationEngine) {
            nySøknad(
                requestBody = request,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(2)
    fun `Hent Søknad, tom er satt till dato då søker fyller 70`() {
        with(testApplicationEngine) {
            hentBehandling(
                behandlingId = behandlingId,
                forventetResponse = forventetResponseHentVedtak
            )
        }
    }

    private companion object {
        private const val saksnummer = "123"
        private const val behandlingId = "456"

        @Language("JSON")
        val forventetResponseHentVedtak = """
            {
              "vedtak": [{
                  "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "2020-01-01"
                  },
                  "behandlingId": "$behandlingId",
                  "gyldigFraOgMed": "2021-01-01",
                  "gyldigTilOgMed": "2025-01-01",
                  "status": "FORSLAG",
                  "uløsteAksjonspunkter": {
                    "LEGEERKLÆRING": {}
                  },
                  "løsteAksjonspunkter": {}
                  }]
            }
        """.trimIndent()
    }
}