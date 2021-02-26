package no.nav.omsorgsdager.saksnummer

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
internal class OmsorgspengerSaksnummerGatewayTest(
    applicationContextBuilder: ApplicationContext.Builder) {

    private val applicationContext = applicationContextBuilder.build()

    @Test
    fun `Person som ikke har vedtak`() {
        val personIdentSaksnummer = "11111111111"
        val expectedResponse = """
            { "saksnummer": "a1b2c3" }
        """.trimIndent()

        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertSaksnummer(
                identitetsnummer = personIdentSaksnummer,
                forventetHttpStatusCode = HttpStatusCode.OK,
                forventetResponse = expectedResponse
            )
        }
    }


    private companion object {

        fun TestApplicationEngine.hentOgAssertSaksnummer(
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