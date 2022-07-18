package no.nav.omsorgsdager

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import okio.Utf8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class HealthApiTest(
    applicationContextBuilder: ApplicationContext.Builder
) {
    private val applicationContext = applicationContextBuilder.build()

    @Test
    fun `Test health end point`() = testApplication {
        application { omsorgsdager(applicationContext) }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, this.status)
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), this.contentType())
        }

    }
}