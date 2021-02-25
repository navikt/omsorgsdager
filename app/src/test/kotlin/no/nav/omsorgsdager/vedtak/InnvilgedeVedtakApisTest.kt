package no.nav.omsorgsdager.vedtak

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.omsorgsdager
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(ApplicationContextExtension::class)
internal class InnvilgedeVedtakApisTest(
    applicationContextBuilder: ApplicationContext.Builder) {

    private val applicationContext = applicationContextBuilder.also {
        it.innvilgedeVedtakService = mockk<InnvilgedeVedtakService>().also {
            coEvery { it.hentInnvilgedeVedtak(eq(IdentitetsnummerUtenVedtak.somIdentitetsnummer()), any(), any()) }.returns(InnvilgedeVedtak.ingenInnvilgedeVedtak())
            coEvery { it.hentInnvilgedeVedtak(eq(IdentitetsnummerMedToAvHver.somIdentitetsnummer()), any(), any()) }.returns(InnvilgedeVedtak(
                kroniskSyktBarn = listOf(
                    KroniskSyktBarnInnvilgetVedtak(barn = Barn(identitetsnummer = "11111111111", fødselsdato = LocalDate.parse("2020-01-01")), tidspunkt = ZonedDateTime.parse("2020-11-10T12:00:00.00Z"), periode = Periode("2020-01-01/2020-12-31"), kilder = setOf(Kilde(id ="1", type = "K9-Sak"))),
                    KroniskSyktBarnInnvilgetVedtak(barn = Barn(identitetsnummer = null, fødselsdato = LocalDate.parse("2019-01-01")), tidspunkt = ZonedDateTime.parse("2021-02-19T23:30:00.00Z"), periode = Periode("2018-01-01/2025-12-31"), kilder = setOf(Kilde(id ="1", type = "Infotrygd")))
                ),
                midlertidigAlene = listOf(
                    MidlertidigAleneInnvilgetVedtak(tidspunkt = ZonedDateTime.parse("1999-11-10T12:00:00.00Z"), periode = Periode(LocalDate.parse("2005-01-01")), kilder = setOf(Kilde(id = "3", type = "Infotrygd"))),
                    MidlertidigAleneInnvilgetVedtak(tidspunkt = ZonedDateTime.parse("2020-11-10T12:00:00.00Z"), periode = Periode("2020-05-05/2030-03-03"), kilder = setOf(Kilde(id = "4", type = "K9-Sak")))
                )
            ))

        }
    }.buildStarted()

    @Test
    fun `Ingen authorization header`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerUtenVedtak,
                authorizationHeader = null,
                forventetHttpStatusCode = HttpStatusCode.Unauthorized
            )
        }
    }

    @Test
    fun `Ikke autorisert system`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerUtenVedtak,
                authorizationHeader = Azure.V2_0.generateJwt(
                    clientId = "k9-sak",
                    audience = "omsorgsdager",
                    accessAsApplication = false
                ).let { "Bearer $it" },
                forventetHttpStatusCode = HttpStatusCode.Forbidden,
                forventetResponse = ForventetResponseForbidden
            )
        }
    }

    @Test
    fun `Ikke autorisert bruker`() {
    }

    @Test
    fun `Ugyldig request`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = "identitetsnummer",
                fom = "tom",
                tom = "tom",
                forventetHttpStatusCode = HttpStatusCode.BadRequest
            )
        }
    }

    @Test
    fun `Person som ikke har vedtak`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerUtenVedtak,
                forventetResponse = ForventetResponseUtenVedtak
            )
        }
    }

    @Test
    fun `Person som ikke har 2 av hvert vedtak`() {
        @Language("JSON")
        val forventetResponse = """
        {
            "kroniskSyktBarn": [{
                "barn": {
                    "identitetsnummer": "11111111111",
                    "fødselsdato": "2020-01-01"
                },
                "kilder": [{
                    "id": "1",
                    "type": "K9-Sak"
                }],
                "vedtatt": "2020-11-10",
                "gyldigFraOgMed": "2020-01-01",
                "gyldigTilOgMed": "2020-12-31"
            }, {
                "barn": {
                    "identitetsnummer": null,
                    "fødselsdato": "2019-01-01"
                },
                "kilder": [{
                    "id": "1",
                    "type": "Infotrygd"
                }],
                "vedtatt": "2021-02-20",
                "gyldigFraOgMed": "2018-01-01",
                "gyldigTilOgMed": "2025-12-31"
            }],
            "midlertidigAlene": [{
                "kilder": [{
                    "id": "3",
                    "type": "Infotrygd"
                }],
                "vedtatt": "1999-11-10",
                "gyldigFraOgMed": "2005-01-01",
                "gyldigTilOgMed": "2005-01-01"
            }, {
                "kilder": [{
                    "id": "4",
                    "type": "K9-Sak"
                }],
                "vedtatt": "2020-11-10",
                "gyldigFraOgMed": "2020-05-05",
                "gyldigTilOgMed": "2030-03-03"
            }]
        }
        """.trimIndent()
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerMedToAvHver,
                forventetResponse = forventetResponse
            )
        }
    }


    private companion object {
        val IdentitetsnummerMedToAvHver = "29099011110"
        val IdentitetsnummerUtenVedtak = "29099011111"
        val ForventetResponseUtenVedtak = """{"kroniskSyktBarn": [], "midlertidigAlene": []}"""
        val ForventetResponseForbidden = """{"detail":"Requesten inneholder ikke tilstrekkelige tilganger.","instance":"about:blank","type":"/problem-details/unauthorized","title":"unauthorized","status":403}"""

        fun TestApplicationEngine.hentOgAssertInnvilgedeVedtak(
            identitetsnummer: String,
            fom: String = "2021-01-01",
            tom: String = "2021-12-31",
            authorizationHeader: String? = Azure.V2_0.generateJwt(
                clientId = "k9-aarskvantum",
                audience = "omsorgsdager",
                accessAsApplication = true
            ).let { "Bearer $it" },
            forventetHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
            forventetResponse: String? = null
        ) {
            with(this) {
                handleRequest(HttpMethod.Post, "/api/innvilgede-vedtak-utvidet-rett") {
                    @Language("JSON")
                    val body = """
                        {
                          "identitetsnummer": "$identitetsnummer",
                          "fom": "$fom",
                          "tom": "$tom"
                        }
                    """.trimIndent()
                    authorizationHeader?.let {
                        addHeader(HttpHeaders.Authorization, authorizationHeader)
                    }
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    setBody(body)
                }.apply {
                    assertEquals(forventetHttpStatusCode, response.status())
                    if (forventetResponse == null) {
                        assertNull(response.content)
                    } else {
                        assertEquals(forventetResponse.somJson(), response.content!!.somJson())
                    }
                }
            }
        }
    }
}