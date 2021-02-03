package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class RevurderingflytTest(
    private val testApplicationEngine: TestApplicationEngine){

    @Test
    @Order(1)
    fun `Sende inn søknad`() {
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
                requestBody = request(behandlingId = behandlingId1),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(2)
    fun `Deaktivere vedtaket`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "DEAKTIVERT",
                "potensielleStatuser": [],
                "uløsteAksjonspunkter": {
                  "LEGEERKLÆRING": {}
                }
            }
            """.trimIndent()
        with(testApplicationEngine) {
            deaktiver(
                behandlingId = behandlingId1,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(3)
    fun `Sender inn søknad på nytt`() {
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
                requestBody = request(behandlingId = behandlingId2),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(4)
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
    @Order(5)
    fun `Fastsetter vedtaket`() {
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
                behandlingId = behandlingId2,
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(6)
    fun `Sender inn søknad på nytt med annen mottatt-dato`() {
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
                    mottatt = "2021-03-30T12:00:00.000+02",
                    behandlingId = behandlingId3
                ),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(7)
    fun `Løse aksjonspunkt for legeerklæring på nytt`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "FORSLAG",
                "potensielleStatuser": ["DEAKTIVERT"],
                "uløsteAksjonspunkter": {}
            }
          """.trimIndent()

        with(testApplicationEngine) {
            aksjonspunkter(
                behandlingId = behandlingId3,
                requestBody = løseAksjonspunktForLegeerklæringRequest(
                    barnetErKroniskSyktEllerHarEnFunksjonshemning = true,
                    erSammenhengMedSøkersRisikoForFraværeFraArbeid = false
                ),
                forventetResponse = forventetResponse
            )
        }
    }

    @Test
    @Order(8)
    fun `Deaktiverer vedtaket på nytt`() {
        @Language("JSON")
        val forventetResponse = """
            {
                "status": "DEAKTIVERT",
                "potensielleStatuser": [],
                "uløsteAksjonspunkter": {}
            }
            """.trimIndent()
        with(testApplicationEngine) {
            deaktiver(
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
                        "barn": {
                            "identitetsnummer": "123",
                            "fødselsdato": "2020-01-01"
                        },
                        "behandlingId": "$behandlingId1",
                        "gyldigFraOgMed": "2021-01-01",
                        "gyldigTilOgMed": "2038-12-31",
                        "status": "DEAKTIVERT",
                        "uløsteAksjonspunkter": {
                            "LEGEERKLÆRING": {}
                        },
                        "løsteAksjonspunkter": {}
                    }]
                }
                """.trimIndent()
            )
            hentBehandling(
                behandlingId = behandlingId2,
                forventetResponse = """
                {
                    "vedtak": [{
                        "barn": {
                            "identitetsnummer": "123",
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
                    }]
                }
                """.trimIndent()
            )
            hentBehandling(
                behandlingId = behandlingId3,
                forventetResponse = """
                {
                    "vedtak": [{
                        "barn": {
                            "identitetsnummer": "123",
                            "fødselsdato": "2020-01-01"
                        },
                        "behandlingId": "$behandlingId3",
                        "gyldigFraOgMed": "2021-03-30",
                        "gyldigTilOgMed": "2038-12-31",
                        "status": "DEAKTIVERT",
                        "uløsteAksjonspunkter": {},
                        "løsteAksjonspunkter": {
                            "LEGEERKLÆRING": {
                                "vurdering": "foo bar",
                                "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                                "erSammenhengMedSøkersRisikoForFraværeFraArbeid": false
                            }
                        }
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
                "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "2020-01-01"
                },
                "behandlingId": "$behandlingId3",
                "gyldigFraOgMed": "2021-03-30",
                "gyldigTilOgMed": "2038-12-31",
                "status": "DEAKTIVERT",
                "uløsteAksjonspunkter": {},
                "løsteAksjonspunkter": {
                    "LEGEERKLÆRING": {
                        "vurdering": "foo bar",
                        "barnetErKroniskSyktEllerHarEnFunksjonshemning": true,
                        "erSammenhengMedSøkersRisikoForFraværeFraArbeid": false
                    }
                }
            }, {
                "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "2020-01-01"
                },
                "behandlingId": "$behandlingId2",
                "gyldigFraOgMed": "2021-01-01",
                "gyldigTilOgMed": "2021-03-29",
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
        with(testApplicationEngine) {
            hentSak(
                saksnummer = saksnummer,
                forventetResponse = forventetResponse
            )
        }
    }

    private companion object {
        val saksnummer = "S-1"
        val behandlingId1 = "B-1"
        val behandlingId2 = "B-2"
        val behandlingId3 = "B-3"

        @Language("JSON")
        fun request(
            mottatt: String = "2020-12-31T23:59:59.000Z",
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
                    "identitetsnummer": "123",
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