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
    fun `Oppretter nytt vedtak`() {

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
            "potensielleStatuser": {
              "INNVILGET": {},
              "DEAKTIVERT": {}, 
              "AVSLÅTT": {}
            },
            "uløsteAksjonspunkter": {
                "LEGEERKLÆRING": {}
            }
        }""".trimIndent()

        with(testApplicationEngine) {
            nyttVedtak(
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
            "potensielleStatuser": {
              "INNVILGET": {},
              "DEAKTIVERT": {}, 
              "AVSLÅTT": {}
            },
            "uløsteAksjonspunkter": {}
        }
      """.trimIndent()

        with(testApplicationEngine) {
            aksjonspunkt(
                behandlingId = behandlingId,
                requestBody = løseAksjonspunktForLegeerklæringRequest,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(3)
    fun `Innvilge vedtaket`() {
        @Language("JSON")
        val forventetResponse = """
        {
            "status": "INNVILGET",
            "potensielleStatuser": {},
            "uløsteAksjonspunkter": {}
        }
        """.trimIndent()
        with(testApplicationEngine) {
            innvilgelse(
                behandlingId = behandlingId,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(4)
    fun `Ikke mulig å endre vedtaket etter at det er innvilget`() {
        with(testApplicationEngine) {
            deaktivering(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            innvilgelse(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            avslag(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            aksjonspunkt(
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
                  "status": "INNVILGET",
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