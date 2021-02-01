package no.nav.omsorgsdager.utvidetrett

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.time.ZonedDateTime

@ExtendWith(TestApplicationExtension::class)
internal class KronisktSyktBarnPostTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    @Test
    fun `UtvidetRett post request med ugyldig body returns 400`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/kroniskt-sykt-barn") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer "+Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager"))
                setBody(
                    """
                        {
                         "mottatt": "Ikke dato",
                         "søker": 11111,
                        }
                    """.trimIndent()
                )
            }.apply {
                assert(response.status() == HttpStatusCode.BadRequest)
            }
        }
    }

    @Test
    fun `Post uten gyldig bearer token gir 403`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/kroniskt-sykt-barn") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization,
                    "Bearer "+Azure.V2_0.generateJwt("omsorgsdager", "noenannen"))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `Post uten authorization gir 401`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/kroniskt-sykt-barn") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `Gyldig post ger forvented respons`() {
        @Language("JSON")
        val payload = """
            {
                "saksnummer": "test",
                "behandlingId": "test",
                "mottatt": "${ZonedDateTime.now()}",
                "søker": {
                    "identitetsnummer": "123",
                    "fødselsdato": "${LocalDate.now().minusYears(30)}",
                    "jobberINorge": true
                },
                "barn": {
                    "identitetsnummer": "123",
                    "fødselsdato": "${LocalDate.now().minusYears(1)}"
                }
            }
        """.trimIndent()

        @Language("JSON")
        val expectedJson = """
            {
                "status": "FORSLAG",
                "uløsteAksjonspunkter": {
                    "VURDERE_LEGEERKLÆRING": {}
                }
            }
        """.trimIndent()

        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/kroniskt-sykt-barn") {
                addHeader(HttpHeaders.Authorization,
                    "Bearer "+Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager"))
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(payload)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                JSONAssert.assertEquals(expectedJson, response.content, false)
            }
        }

    }
}