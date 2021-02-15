package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SøknadMedBarnUtenIdentitetsnummerTest(
    private val testApplicationEngine: TestApplicationEngine) {

    @Test
    @Order(1)
    fun `Oppretter nytt vedtak`() {

        @Language("JSON")
        val forventetResponse = """
        {
            "status": "FORESLÅTT",
            "potensielleStatuser": {
              "INNVILGET": {},
              "FORKASTET": {}, 
              "AVSLÅTT": {}
            },
            "uløsteBehov": {
                "VURDERE_KRONISK_SYKT_BARN": {}
            }
        }""".trimIndent()

        with(testApplicationEngine) {
            nyttVedtak(
                requestBody = opprettRequest,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(2)
    fun `Løse behov for legeerklæring`() {
        @Language("JSON")
        val forventetResponse = """
        {
            "status": "FORESLÅTT",
            "potensielleStatuser": {
              "INNVILGET": {},
              "FORKASTET": {}, 
              "AVSLÅTT": {}
            },
            "uløsteBehov": {}
        }
      """.trimIndent()

        with(testApplicationEngine) {
            løs(
                behandlingId = behandlingId,
                requestBody = løseBehovForLegeerklæringRequest,
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
            "uløsteBehov": {}
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
    @Order(5)
    fun `Hente behandlingen`() {
        with(testApplicationEngine) {
            hentBehandling(
                behandlingId = behandlingId,
                forventetResponse = forventetResponseHentVedtak
            )
        }
    }

    private companion object {
        private val saksnummer = UUID.randomUUID().toString()
        private val behandlingId = UUID.randomUUID().toString()

        @Language("JSON")
        private val opprettRequest = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "søknadMottatt": "2020-12-31T23:59:59.000Z",
                "tidspunkt": "2021-01-01T12:00:00.000Z",
                "søker": {
                    "identitetsnummer": "123"
                },
                "barn": {
                    "fødselsdato": "2020-01-01",
                    "harSammeBosted": true
                }
            }
        """.trimIndent()

        @Language("JSON")
        private val løseBehovForLegeerklæringRequest = """
            {
              "VURDERE_KRONISK_SYKT_BARN": {
                "vurdering": "foo bar",
                "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                "erSammenhengMedSøkersRisikoForFraværFraArbeid": true
              }
            }
            """.trimIndent()

        @Language("JSON")
        val forventetResponseHentVedtak = """
        {
            "vedtak": [{
                "behandlingId": "$behandlingId",
                "gyldigFraOgMed": "2021-01-01",
                "gyldigTilOgMed": "2038-12-31",
                "status": "INNVILGET",
                "uløsteBehov": {},
                "løsteBehov": {
                    "VURDERE_PERIODE_FOR_KRONISK_SYKT_BARN": {
                        "grunnlag": {
                          "søknadMottatt": "2021-01-01",
                          "sisteDagIÅretBarnetFyller18": "2038-12-31"
                        },
                        "løsning": {
                            "fom": "2021-01-01",
                            "tom": "2038-12-31"
                        },
                        "lovanvendelser": {
                            "innvilget": {
                                "Ftrl. § 9-5 fjerde ledd andre punktum": ["Perioden gjelder fra dagen søknaden ble mottatt ut året barnet fyller 18 år."]
                            },
                            "avslått": {}
                        }
                    },
                    "VURDERE_KRONISK_SYKT_BARN": {
                        "grunnlag": {},
                        "løsning": {
                            "vurdering": "foo bar",
                            "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                            "erSammenhengMedSøkersRisikoForFraværFraArbeid": true
                        },
                        "lovanvendelser": {
                            "innvilget": {
                                "Ftrl. § 9-6 andre ledd": ["Barnet er kronisk sykt eller har en funksjonshemning.", "Er sammenheng med søkers risiko for fravær fra arbeidet."]
                            },
                            "avslått": {}
                        }
                    }
                },
                "grunnlag": $opprettRequest
            }]
        }
        """.trimIndent()
    }
}