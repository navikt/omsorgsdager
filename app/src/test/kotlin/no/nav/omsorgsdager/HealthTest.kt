package no.nav.omsorgsdager

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import no.nav.omsorgsdager.testutils.TestApplicationExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class HealthTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    @Test
    fun `isready gir 200`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assert(response.status() == HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `isalive gir 200`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                assert(response.status() == HttpStatusCode.OK)
            }
        }
    }
}
