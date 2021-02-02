package no.nav.omsorgsdager.utvidetrett

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime

@ExtendWith(TestApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class NormalflytInngvilgetSøknadTest(
    private val testApplicationEngine: TestApplicationEngine) {

    @Test
    @Order(1)
    fun `Sende inn søknad`() {

        @Language("JSON")
        val request = """
            {
                "saksnummer": "123",
                "behandlingId": "$behandlingId",
                "mottatt": "${ZonedDateTime.now()}",
                "søker": {
                    "identitetsnummer": "123",
                    "fødselsdato": "${LocalDate.now().minusYears(30)}",
                    "jobberINorge": true
                },
                "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "${LocalDate.now().minusYears(1)}"
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
    fun `Løse aksjonspunkt for legeerklæring`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "FORSLAG",
                "uløsteAksjonspunkter": {}
            }
          """.trimIndent()

        with(testApplicationEngine) {
            aksjonspunkter(
                behandlingId = behandlingId,
                requestBody = løseAksjonspunktForLegeerklæringRequest,
                forventetResponse = forventetResponse
            )
        }
    }


    @Test
    @Order(3)
    fun `Fastsette vedtaket`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "FASTSATT",
                "uløsteAksjonspunkter": {}
            }
            """.trimIndent()
        with(testApplicationEngine) {
            fastsett(
                behandlingId = behandlingId,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(4)
    fun `Ikke mulig å endre vedtaket etter at det er fastsatt`() {
        with(testApplicationEngine) {
            deaktiver(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            fastsett(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            aksjonspunkter(
                behandlingId = behandlingId,
                requestBody = løseAksjonspunktForLegeerklæringRequest,
                forventetStatusCode = HttpStatusCode.Conflict
            )
        }
    }

    private companion object {
        private const val behandlingId = "456"
        @Language("JSON")
        private val løseAksjonspunktForLegeerklæringRequest = """
            {
              "LEGEERKLÆRING": {
                    "begrunnelse": "foo bar",
                    "barnetErKroniskSykt": true,
                    "barnetErFunksjonshemmet": false
                }
            }
            """.trimIndent()
    }
}