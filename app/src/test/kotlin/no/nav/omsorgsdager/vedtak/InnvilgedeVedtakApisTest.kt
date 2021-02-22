package no.nav.omsorgsdager.vedtak

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(TestApplicationExtension::class)
internal class InnvilgedeVedtakApisTest(
    private val testApplicationEngine: TestApplicationEngine) {


    @Test
    fun `Ingen authorization header`() {
        testApplicationEngine.hentOgAssert(
            identitetsnummer = IdentitetsnummerUtenVedtak,
            authorizationHeader = null,
            forventetHttpStatusCode = HttpStatusCode.Unauthorized
        )
    }

    @Test
    fun `Ikke autorisert system`() {
        testApplicationEngine.hentOgAssert(
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

    @Test
    fun `Ikke autorisert bruker`() {
    }

    @Test
    fun `Ugyldig request`() {
        testApplicationEngine.hentOgAssert(
            identitetsnummer = "identitetsnummer",
            fom = "tom",
            tom = "tom",
            forventetHttpStatusCode = HttpStatusCode.BadRequest
        )
    }

    @Test
    fun `Person som ikke har vedtak hverken i Infotrygd eller K9-Sak`() {
        testApplicationEngine.hentOgAssert(
            identitetsnummer = IdentitetsnummerUtenVedtak,
            forventetResponse = ForventetResponseUtenVedtak
        )
    }


    private companion object {
        val IdentitetsnummerUtenVedtak = "29099011111"
        val ForventetResponseUtenVedtak = """{"kroniskSyktBarn": [], "midlertidigAlene": []}"""
        val ForventetResponseForbidden = """{"detail":"Requesten inneholder ikke tilstrekkelige tilganger.","instance":"about:blank","type":"/problem-details/unauthorized","title":"unauthorized","status":403}"""

        fun TestApplicationEngine.hentOgAssert(
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