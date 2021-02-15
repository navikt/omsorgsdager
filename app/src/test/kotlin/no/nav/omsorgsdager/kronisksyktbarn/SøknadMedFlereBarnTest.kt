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
    fun `Sende inn vedtak med barn 123 (behandling1)`() {
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
                requestBody = opprettRequest1,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(2)
    fun `Sende inn vedtak med barn 456 (behandling2)`() {
        @Language("JSON")
        val forventetResponse = """
        {
            "status": "FORESLÅTT",
            "potensielleStatuser": {
              "INNVILGET": {},
              "AVSLÅTT": {},
              "FORKASTET": {}
            },
            "uløsteBehov": {
                "VURDERE_KRONISK_SYKT_BARN": {}
            }
        }""".trimIndent()

        with(testApplicationEngine) {
            nyttVedtak(
                requestBody = opprettRequest2,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(3)
    fun `Løse behov for legeerklæring for behandling1 & behandling2`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "FORESLÅTT",
                "potensielleStatuser": {
                  "INNVILGET": {},
                  "AVSLÅTT": {},
                  "FORKASTET": {}
                },
                "uløsteBehov": {}
            }
          """.trimIndent()

        with(testApplicationEngine) {
            løs(
                behandlingId = behandlingId1,
                requestBody = løseBehovForLegeerklæringRequest(
                    barnetErKroniskSyktEllerHarEnFunksjonshemning = true,
                    erSammenhengMedSøkersRisikoForFraværFraArbeid = true
                ),
                forventetResponse = forventetResponse
            )
            løs(
                behandlingId = behandlingId2,
                requestBody = løseBehovForLegeerklæringRequest(
                    barnetErKroniskSyktEllerHarEnFunksjonshemning = true,
                    erSammenhengMedSøkersRisikoForFraværFraArbeid = true
                ),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(4)
    fun `Innvilger vedtak 1 & 2`() {
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
                behandlingId = behandlingId1,
                forventetResponse = forventetResponse
            )
            innvilgelse(
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
            "vedtak": [{
                "behandlingId": "$behandlingId2",
                "gyldigFraOgMed": "2021-01-01",
                "gyldigTilOgMed": "2038-12-31",
                "status": "INNVILGET",
                "uløsteBehov": {},
                "løsteBehov": {
                    "VURDERE_PERIODE_FOR_KRONISK_SYKT_BARN": {
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
                "grunnlag": $opprettRequest2
            }, {
                "behandlingId": "$behandlingId1",
                "gyldigFraOgMed": "2021-01-01",
                "gyldigTilOgMed": "2038-12-31",
                "status": "INNVILGET",
                "uløsteBehov": {},
                "løsteBehov": {
                    "VURDERE_PERIODE_FOR_KRONISK_SYKT_BARN": {
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
                "grunnlag": $opprettRequest1
            }]
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

        private val opprettRequest1 = opprettRequest(
            behandlingId = behandlingId1,
            barnIdentitetsnummer = "123"
        )

        private val opprettRequest2 = opprettRequest(
            behandlingId = behandlingId2,
            barnIdentitetsnummer = "456"
        )

        @Language("JSON")
        private fun opprettRequest(
            mottatt: String = "2020-12-31T23:59:59.000Z",
            barnIdentitetsnummer: String = "123",
            behandlingId: String) = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "søknadMottatt": "$mottatt",
                "tidspunkt": "2021-01-01T12:00:00.000Z",
                "søker": {
                    "identitetsnummer": "123"
                },
                "barn": {
                    "identitetsnummer": "$barnIdentitetsnummer",
                    "fødselsdato": "2020-01-01",
                    "harSammeBosted": true
                }
            }
        """.trimIndent()

        @Language("JSON")
        private fun løseBehovForLegeerklæringRequest(
            barnetErKroniskSyktEllerHarEnFunksjonshemning: Boolean,
            erSammenhengMedSøkersRisikoForFraværFraArbeid: Boolean
        ) = """
            {
              "VURDERE_KRONISK_SYKT_BARN": {
                "vurdering": "foo bar",
                "barnetErKroniskSyktEllerHarEnFunksjonshemning": $barnetErKroniskSyktEllerHarEnFunksjonshemning,
                "erSammenhengMedSøkersRisikoForFraværFraArbeid": $erSammenhengMedSøkersRisikoForFraværFraArbeid
              }
            }
            """.trimIndent()
    }
}