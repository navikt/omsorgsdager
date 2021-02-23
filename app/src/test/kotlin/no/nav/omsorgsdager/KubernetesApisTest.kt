package no.nav.omsorgsdager

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class KubernetesApisTest(
    applicationContextBuilder: ApplicationContext.Builder) {
    private val applicationContext = applicationContextBuilder.build()

    @Test
    fun `isready gir 200`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assert(response.status() == HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `isalive gir 200`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                assert(response.status() == HttpStatusCode.OK)
            }
        }
    }
}
