package no.nav.omsorgsdager

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class UtvidetRettAksjonsTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    @Test
    fun `Gyldig post ger forvented respons`() {

        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/utvidet-rett/aksjonspunkt") {
                addHeader(HttpHeaders.Authorization,
                    "Bearer "+Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager"))
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("{}")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.NotImplemented, response.status())
            }
        }

    }
}