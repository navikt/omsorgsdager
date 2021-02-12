package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime

@ExtendWith(TestApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class RevurderingflytTest(
    private val testApplicationEngine: TestApplicationEngine){

    @Test
    @Order(1)
    fun `Opprett nytt vedtak`() {
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
                "LEGEERKLÆRING": {}
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
    fun `Avslå vedtaket`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "AVSLÅTT",
                "potensielleStatuser": {},
                "uløsteBehov": {
                  "LEGEERKLÆRING": {}
                }
            }
            """.trimIndent()
        with(testApplicationEngine) {
            avslag(
                behandlingId = behandlingId1,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(3)
    fun `Oppretter vedtaket andre gang`() {
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
                "LEGEERKLÆRING": {}
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
    @Order(4)
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
    @Order(5)
    fun `Innvilger vedtaket`() {
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
                behandlingId = behandlingId2,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(6)
    fun `Oppretter vedtaket en tredje gang, nå med annen mottatt-dato`() {
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
                "LEGEERKLÆRING": {}
            }
        }""".trimIndent()

        with(testApplicationEngine) {
            nyttVedtak(
                requestBody = opprettRequest3,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(7)
    fun `Løse behov for legeerklæring på nytt`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "FORESLÅTT",
                "potensielleStatuser": {
                  "FORKASTET": {}, 
                  "AVSLÅTT": {}
                },
                "uløsteBehov": {}
            }
          """.trimIndent()

        with(testApplicationEngine) {
            løs(
                behandlingId = behandlingId3,
                requestBody = løseBehovForLegeerklæringRequest(
                    barnetErKroniskSyktEllerHarEnFunksjonshemning = true,
                    erSammenhengMedSøkersRisikoForFraværFraArbeid = false
                ),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(8)
    fun `Avslår vedtaket på nytt`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "AVSLÅTT",
                "potensielleStatuser": {},
                "uløsteBehov": {}
            }
            """.trimIndent()
        with(testApplicationEngine) {
            avslag(
                behandlingId = behandlingId3,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(9)
    fun `Henter hvert enkelt vedtak`() {
        with(testApplicationEngine) {
            hentBehandling(
                behandlingId = behandlingId1,
                forventetResponse = """
                {
                    "vedtak": [{
                        "behandlingId": "$behandlingId1",
                        "gyldigFraOgMed": "2021-01-01",
                        "gyldigTilOgMed": "2038-12-31",
                        "status": "AVSLÅTT",
                        "uløsteBehov": {
                            "LEGEERKLÆRING": {}
                        },
                        "løsteBehov": {},
                        "grunnlag": $opprettRequest1
                    }]
                }
                """.trimIndent()
            )
            hentBehandling(
                behandlingId = behandlingId2,
                forventetResponse = """
                {
                    "vedtak": [{
                        "behandlingId": "$behandlingId2",
                        "gyldigFraOgMed": "2021-01-01",
                        "gyldigTilOgMed": "2038-12-31",
                        "status": "INNVILGET",
                        "uløsteBehov": {},
                        "løsteBehov": {
                            "LEGEERKLÆRING": {
                                "vurdering": "foo bar",
                                "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                                "erSammenhengMedSøkersRisikoForFraværFraArbeid": true
                            }
                        },
                        "grunnlag": $opprettRequest2
                    }]
                }
                """.trimIndent()
            )
            hentBehandling(
                behandlingId = behandlingId3,
                forventetResponse = """
                {
                    "vedtak": [{
                        "behandlingId": "$behandlingId3",
                        "gyldigFraOgMed": "2021-03-30",
                        "gyldigTilOgMed": "2038-12-31",
                        "status": "AVSLÅTT",
                        "uløsteBehov": {},
                        "løsteBehov": {
                            "LEGEERKLÆRING": {
                                "vurdering": "foo bar",
                                "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                                "erSammenhengMedSøkersRisikoForFraværFraArbeid": false
                            }
                        },
                        "grunnlag": $opprettRequest3
                    }]
                }
                """.trimIndent()
            )
        }
    }

    @Test
    @Order(10)
    fun `Henter vedtak aggregert på saksnummer`() {

        @Language("JSON")
        val forventetResponse = """
        {
            "vedtak": [{
                "behandlingId": "$behandlingId3",
                "gyldigFraOgMed": "2021-03-30",
                "gyldigTilOgMed": "2038-12-31",
                "status": "AVSLÅTT",
                "uløsteBehov": {},
                "løsteBehov": {
                    "LEGEERKLÆRING": {
                        "vurdering": "foo bar",
                        "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                        "erSammenhengMedSøkersRisikoForFraværFraArbeid": false
                    }
                },
                "grunnlag": $opprettRequest3
            }, {
                "behandlingId": "$behandlingId2",
                "gyldigFraOgMed": "2021-01-01",
                "gyldigTilOgMed": "2021-03-29",
                "status": "INNVILGET",
                "uløsteBehov": {},
                "løsteBehov": {
                    "LEGEERKLÆRING": {
                        "vurdering": "foo bar",
                        "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                        "erSammenhengMedSøkersRisikoForFraværFraArbeid": true
                    }
                },
                "grunnlag": $opprettRequest2
            }]
        }
        """.trimIndent()
        with(testApplicationEngine) {
            hentSak(
                saksnummer = saksnummer,
                forventetResponse = forventetResponse
            )
        }
    }

    private companion object {
        private val saksnummer = "S-1"
        private val behandlingId1 = "B-1"
        private val behandlingId2 = "B-2"
        private val behandlingId3 = "B-3"

        private val opprettRequest1 = opprettRequest(behandlingId = behandlingId1)
        private val opprettRequest2 = opprettRequest(behandlingId = behandlingId2)
        private val opprettRequest3 = opprettRequest(
            mottatt = "2021-03-30T12:00:00.000+02",
            behandlingId = behandlingId3
        )


        @Language("JSON")
        private fun opprettRequest(
            mottatt: String = "2020-12-31T23:59:59.000Z",
            behandlingId: String) = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "søknadMottatt": "$mottatt",
                "tidspunkt": "${ZonedDateTime.parse(mottatt).plusMinutes(10)}",
                "søker": {
                    "identitetsnummer": "123",
                    "fødselsdato": "1990-01-01"
                },
                "barn": {
                    "identitetsnummer": "123",
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
              "LEGEERKLÆRING": {
                "vurdering": "foo bar",
                "barnetErKroniskSyktEllerHarEnFunksjonshemning": $barnetErKroniskSyktEllerHarEnFunksjonshemning,
                "erSammenhengMedSøkersRisikoForFraværFraArbeid": $erSammenhengMedSøkersRisikoForFraværFraArbeid
              }
            }
            """.trimIndent()
    }
}