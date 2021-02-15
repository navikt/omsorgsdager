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
                requestBody = oppprettRequest,
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
    @Order(4)
    fun `Ikke mulig å endre vedtaket etter at det er innvilget`() {
        @Language("JSON")
        val forventetResponse = """
        {
            "status": "INNVILGET",
            "potensielleStatuser": {},
            "uløsteBehov": {}
        }
        """.trimIndent()
        with(testApplicationEngine) {
            forkast(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            innvilgelse(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.OK,
                forventetResponse = forventetResponse
            )
            avslag(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            løs(
                behandlingId = behandlingId,
                requestBody = løseBehovForLegeerklæringRequest,
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
        with(testApplicationEngine) {
            nyttVedtak(
                requestBody = oppprettRequest,
                forventetStatusCode = HttpStatusCode.Conflict
            )
        }
    }

    private companion object {
        private const val saksnummer = "123"
        private const val behandlingId = "456"

        @Language("JSON")
        private val oppprettRequest = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "søknadMottatt": "2020-12-31T23:59:59.000Z",
                "tidspunkt": "2021-01-01T12:00:00.000Z",
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
                  "grunnlag": $oppprettRequest
              }]
            }
        """.trimIndent()
    }
}