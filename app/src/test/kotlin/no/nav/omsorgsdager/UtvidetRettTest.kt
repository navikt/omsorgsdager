package no.nav.omsorgsdager

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
class UtvidetRettTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    @Test
    fun `UtvidetRett post request med ugyldig body returns 401`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/utvidet-rett") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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

}