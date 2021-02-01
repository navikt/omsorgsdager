package no.nav.omsorgsdager.utvidetrett

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert

@ExtendWith(TestApplicationExtension::class)
internal class KronisktSyktBarnAksjonspunktTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    private val accessToken = "Bearer "+Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager")

    @Test
    fun `Gyldig post ger forvented respons`() {
        val body = """
            {
                "LEGEERKLÆRING": {},
                "MEDLEMSKAP": {},
                "YRKESAKTIVITET": {}
            }
        """.trimIndent()
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Put, "/api/kroniskt-sykt-barn/1234/aksjonspunkt") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
            }
        }

    }

    @Test
    fun `Fastsett`() {
        val expectedJson = """
                {
                    "status": "FASTSATT",
                    "uløsteAksjonspunkter": {}
                }
        """.trimIndent()
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Put, "/api/kroniskt-sykt-barn/123/fastsett") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }.apply {
            Assertions.assertEquals(HttpStatusCode.OK, response.status())
            JSONAssert.assertEquals(expectedJson, response.content, true)
        }
    }

    @Test
    fun `Deaktiver`() {
        val expectedJson = """
                {
                    "status": "DEAKTIVERT",
                    "uløsteAksjonspunkter": {}
                }
        """.trimIndent()
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Put, "/api/kroniskt-sykt-barn/123/deaktiver") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }.apply {
            Assertions.assertEquals(HttpStatusCode.OK, response.status())
            JSONAssert.assertEquals(expectedJson, response.content, true)
        }
    }
}