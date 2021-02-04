package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SøknadMedFlereBarnTest(
    private val testApplicationEngine: TestApplicationEngine){

    @Test
    @Order(1)
    fun `Sende inn søknad med barn 123 (behandling1)`() {
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
                requestBody = request(
                    behandlingId = behandlingId1,
                    barnIdentitetsnummer = "123"),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(2)
    fun `Sende inn søknad med barn 456 (behandling2)`() {
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
                requestBody = request(
                    behandlingId = behandlingId2,
                    barnIdentitetsnummer = "456"),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(3)
    fun `Løse aksjonspunkt for legeerklæring for behandling1 & behandling2`() {
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
                behandlingId = behandlingId1,
                requestBody = løseAksjonspunktForLegeerklæringRequest(
                    barnetErKroniskSyktEllerHarEnFunksjonshemning = true,
                    erSammenhengMedSøkersRisikoForFraværeFraArbeid = true
                ),
                forventetResponse = forventetResponse
            )
            aksjonspunkter(
                behandlingId = behandlingId2,
                requestBody = løseAksjonspunktForLegeerklæringRequest(
                    barnetErKroniskSyktEllerHarEnFunksjonshemning = true,
                    erSammenhengMedSøkersRisikoForFraværeFraArbeid = true
                ),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(4)
    fun `Fastsetter vedtak 1 & 2`() {
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
                behandlingId = behandlingId1,
                forventetResponse = forventetResponse
            )
            fastsett(
                behandlingId = behandlingId2,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(5)
    fun `Henter allt knyttet till saksnummer, forventer två barn`() {
        @Language("JSON")
        val forventetResponse = """
            {
              "vedtak": [
                {
                  "barn": {
                    "identitetsnummer": "456",
                    "fødselsdato": "2020-01-01"
                  },
                  "behandlingId": "$behandlingId2",
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
                },
                {
                  "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "2020-01-01"
                  },
                  "behandlingId": "$behandlingId1",
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
                }
              ]
            }
        """.trimIndent()
        with(testApplicationEngine) {
            hentSak(
                saksnummer = saksnummer,
                forventetStatusCode = HttpStatusCode.OK,
                forventetResponse = forventetResponse
            )
        }
    }

    private companion object {
        val saksnummer = UUID.randomUUID().toString()
        val behandlingId1 = UUID.randomUUID().toString()
        val behandlingId2 = UUID.randomUUID().toString()

        @Language("JSON")
        fun request(
            mottatt: String = "2020-12-31T23:59:59.000Z",
            barnIdentitetsnummer: String = "123",
            behandlingId: String) = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "mottatt": "$mottatt",
                "søker": {
                    "identitetsnummer": "123",
                    "fødselsdato": "1990-01-01"
                },
                "barn": {
                    "identitetsnummer": "$barnIdentitetsnummer",
                    "fødselsdato": "2020-01-01"
                }
            }
        """.trimIndent()

        @Language("JSON")
        private fun løseAksjonspunktForLegeerklæringRequest(
            barnetErKroniskSyktEllerHarEnFunksjonshemning: Boolean,
            erSammenhengMedSøkersRisikoForFraværeFraArbeid: Boolean
        ) = """
            {
              "LEGEERKLÆRING": {
                "vurdering": "foo bar",
                "barnetErKroniskSyktEllerHarEnFunksjonshemning": $barnetErKroniskSyktEllerHarEnFunksjonshemning,
                "erSammenhengMedSøkersRisikoForFraværeFraArbeid": $erSammenhengMedSøkersRisikoForFraværeFraArbeid
              }
            }
            """.trimIndent()
    }
}