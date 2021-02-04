package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

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
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "mottatt": "2020-12-31T23:59:59.000Z",
                "søker": {
                    "identitetsnummer": "123",
                    "fødselsdato": "1990-01-01"
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
            "potensielleStatuser": ["FASTSATT", "DEAKTIVERT"],
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
                "potensielleStatuser": ["FASTSATT", "DEAKTIVERT"],
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
                "potensielleStatuser": [],
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

    @Test
    @Order(5)
    fun `Hente behandlingen`() {
        with(testApplicationEngine) {
            hentBehandling(
                behandlingId = behandlingId,
                forventetResponse = forventetResponseHentVedtak
            )
        }
    }

    @Test
    @Order(6)
    fun `Hente saken`() {
        with(testApplicationEngine) {
            hentSak(
                saksnummer = saksnummer,
                forventetResponse = forventetResponseHentVedtak
            )
        }
    }

    @Test
    @Order(7)
    fun `Send in søknad med brukt behandlingsId forvent 409`() {
        @Language("JSON")
        val request = """
            {
                "saksnummer": "123",
                "behandlingId": "$behandlingId",
                "mottatt": "2021-02-01T23:59:59.000Z",
                "søker": {
                    "identitetsnummer": "456",
                    "fødselsdato": "1990-01-01"
                },
                "barn": {
                    "identitetsnummer": "456",
                    "fødselsdato": "2020-01-01"
                }
            }
        """.trimIndent()

        with(testApplicationEngine) {
            nySøknad(
                requestBody = request,
                forventetStatusCode = HttpStatusCode.Conflict
            )
        }
    }

    private companion object {
        private const val saksnummer = "123"
        private const val behandlingId = "456"

        @Language("JSON")
        private val løseAksjonspunktForLegeerklæringRequest = """
            {
              "LEGEERKLÆRING": {
                "vurdering": "foo bar",
                "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                "erSammenhengMedSøkersRisikoForFraværeFraArbeid": true
              }
            }
            """.trimIndent()

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
                  "gyldigTilOgMed": "2038-12-31",
                  "status": "FASTSATT",
                  "uløsteAksjonspunkter": {},
                  "løsteAksjonspunkter": {
                    "LEGEERKLÆRING": {
                        "vurdering": "foo bar",
                        "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                        "erSammenhengMedSøkersRisikoForFraværeFraArbeid": true
                    }
                  }
              }]
            }
        """.trimIndent()
    }
}