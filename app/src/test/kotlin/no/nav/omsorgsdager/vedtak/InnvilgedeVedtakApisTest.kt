package no.nav.omsorgsdager.vedtak

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.omsorgsdager
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(ApplicationContextExtension::class)
internal class InnvilgedeVedtakApisTest(
    applicationContextBuilder: ApplicationContext.Builder
) {
    private val applicationContext = applicationContextBuilder.build()

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
    fun `Person som ikke har vedtak hverken i Infotrygd eller K9-Sak`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerUtenVedtak,
                forventetResponse = ForventetResponseUtenVedtak
            )
        }
    }

    @Test
    fun `Person med utvidetrett i infotrygd`() {
        val IdentitetsnummerMedVedtakInfotrygd = "29099022222"
        @Language("JSON")
        val ForventetResponseMedInfotrygdVedtak = """
            {
               "kroniskSyktBarn":[
                  {
                     "barn": {"identitetsnummer":"01019911111", "fødselsdato":"1999-01-01"},
                     "kilder": [{"id":"UTV.RETT/20D/29099022222",  "type":"Personkort" }]
                  }
               ],
               "midlertidigAlene":[
                  {
                  "kilder":[{"id":"midl.alene.om/17D", "type":"Personkort"}]
                  }
               ]
            }
        """.trimIndent()
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerMedVedtakInfotrygd,
                forventetResponse = ForventetResponseMedInfotrygdVedtak
            )
        }
    }


    private companion object {
        val IdentitetsnummerUtenVedtak = "29099011111"
        val ForventetResponseUtenVedtak = """{"kroniskSyktBarn": [], "midlertidigAlene": []}"""
        val ForventetResponseForbidden =
            """{"detail":"Requesten inneholder ikke tilstrekkelige tilganger.","instance":"about:blank","type":"/problem-details/unauthorized","title":"unauthorized","status":403}"""

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