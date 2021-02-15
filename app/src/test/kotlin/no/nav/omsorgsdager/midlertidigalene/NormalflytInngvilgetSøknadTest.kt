package no.nav.omsorgsdager.midlertidigalene

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

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
                "VURDERE_MIDLERTIDIG_ALENE": {}
            }
        }""".trimIndent()

        with(testApplicationEngine) {
            nyttMidlertidigAleneVedtak(
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
            løsBehovMidlertidigAlene(
                behandlingId = behandlingId,
                requestBody = løseVurdereMidlertidigAleneBehov,
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
            innvilgelseMidlertidigAlene(
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
            forkastMidlertidigAlene(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            innvilgelseMidlertidigAlene(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.OK,
                forventetResponse = forventetResponse
            )
            avslagMidlertidigAlene(
                behandlingId = behandlingId,
                forventetStatusCode = HttpStatusCode.Conflict
            )
            løsBehovMidlertidigAlene(
                behandlingId = behandlingId,
                requestBody = løseVurdereMidlertidigAleneBehov,
                forventetStatusCode = HttpStatusCode.Conflict
            )
        }
    }

    @Test
    @Order(5)
    fun `Hente behandlingen`() {
        with(testApplicationEngine) {
            hentBehandlingMidlertidigAlene(
                behandlingId = behandlingId,
                forventetResponse = forventetResponseHentVedtak
            )
        }
    }

    @Test
    @Order(6)
    fun `Hente saken`() {
        with(testApplicationEngine) {
            hentSakMidlertidigAlene(
                saksnummer = saksnummer,
                forventetResponse = forventetResponseHentVedtak
            )
        }
    }

    @Test
    @Order(7)
    fun `Send in søknad med brukt behandlingsId forvent 409`() {
        with(testApplicationEngine) {
            nyttMidlertidigAleneVedtak(
                requestBody = oppprettRequest,
                forventetStatusCode = HttpStatusCode.Conflict
            )
        }
    }

    private companion object {
        private val saksnummer = UUID.randomUUID().toString()
        private val behandlingId = UUID.randomUUID().toString()

        @Language("JSON")
        private val oppprettRequest = """
            {
                "saksnummer": "$saksnummer",
                "behandlingId": "$behandlingId",
                "søknadMottatt": "2020-12-31T23:59:59.000Z",
                "søker": {
                    "identitetsnummer": "29099011111"
                },
                "motpart": {
                    "identitetsnummer": "29099022222"
                }
            }
        """.trimIndent()

        @Language("JSON")
        private val løseVurdereMidlertidigAleneBehov = """
            {
              "VURDERE_MIDLERTIDIG_ALENE": {
                "vurdering": "foo bar",
                "erSøkerenMidlertidigAleneOmOmsorgen": true,
                "gyldigFraOgMed": "2020-01-01",
                "gyldigTilOgMed": "2025-01-12"
              }
            }
            """.trimIndent()

        @Language("JSON")
        val forventetResponseHentVedtak = """
        {
            "vedtak": [{
                "behandlingId": "$behandlingId",
                "gyldigFraOgMed": "2020-01-01",
                "gyldigTilOgMed": "2025-01-12",
                "status": "INNVILGET",
                "uløsteBehov": {},
                "løsteBehov": {
                    "VURDERE_MIDLERTIDIG_ALENE": {
                        "grunnlag": {},
                        "løsning": {
                            "erSøkerenMidlertidigAleneOmOmsorgen": true,
                            "gyldigFraOgMed": "2020-01-01",
                            "gyldigTilOgMed": "2025-01-12",
                            "vurdering": "foo bar"
                        },
                        "lovanvendelser": {
                            "innvilget": {
                                "Ftrl. § 9-6 tredje ledd": ["Søkeren er midlertidig alene om omsorgen."]
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