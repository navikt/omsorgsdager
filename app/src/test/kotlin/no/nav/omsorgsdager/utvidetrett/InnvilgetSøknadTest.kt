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
import java.time.LocalDateTime
import java.time.ZonedDateTime

@ExtendWith(TestApplicationExtension::class)
internal class InnvilgetSøknadTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    private val accessToken = "Bearer "+Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager")

    @Test
    fun `Innvilget søknad`() {
        @Language("JSON")
        val payload = """
            {
                "saksnummer": "123",
                "behandlingId": "456",
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
        val aksjonspunktRequest = """
            {
              "MEDLEMSKAP": {}
            }
        """.trimIndent()

        val aksjonspunktExpectedJson = """
            {
                "status": "FORSLAG",
                "uløsteAksjonspunkter": {
                    "MEDLEMSKAP": {}
                }
            }
        """.trimIndent()

        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/kroniskt-sykt-barn") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(payload)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Put, "/kroniskt-sykt-barn/456/aksjonspunkt") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(aksjonspunktRequest)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                JSONAssert.assertEquals(aksjonspunktExpectedJson, response.content, true)
            }

            handleRequest(HttpMethod.Put, "/kroniskt-sykt-barn/456/fastsett") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                assert(response.content!!.contains("FASTSATT"))
            }

            handleRequest(HttpMethod.Put, "/kroniskt-sykt-barn/456/deaktiver") {
                addHeader(HttpHeaders.Authorization, accessToken)
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
            }

        }

    }
}