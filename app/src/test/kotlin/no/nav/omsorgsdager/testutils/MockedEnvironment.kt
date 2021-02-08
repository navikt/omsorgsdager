package no.nav.omsorgsdager.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.mockk.mockk
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsJwksUrl
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.app
import no.nav.omsorgsdager.testutils.wiremock.*
import java.io.File
import java.nio.file.Files

internal class MockedEnvironment(
    applicationPort: Int = 8080,
    wireMockPort: Int? = null) {
    private fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
        .setOverrideWorkingDirectory(tempDir)
        .setDataDirectory(tempDir.resolve("datadir"))
        .start()

    private val embeddedPostgres = embeddedPostgress(Files.createTempDirectory("tmp_postgres").toFile())
    private val wireMockServer = WireMockBuilder()
        .withAzureSupport()
        .withNaisStsSupport()
        .let { when (wireMockPort) {
            null -> it
            else -> it.withPort(wireMockPort)
        }}
        .build()
        .stubAccessTokens()
        .stubTilgangApi()
        .stubPdlApi()

    internal val applicationContext = ApplicationContext.Builder(
        env = mapOf(
            "PORT" to "$applicationPort",
            "DATABASE_HOST" to "localhost",
            "DATABASE_PORT" to "${embeddedPostgres.port}",
            "DATABASE_DATABASE" to "postgres",
            "DATABASE_USERNAME" to "postgres",
            "DATABASE_PASSWORD" to "postgres",
            "PDL_BASE_URL" to wireMockServer.pdlApiBaseUrl(),
            "PROXY_SCOPES" to "/.default",
            "TILGANGSSTYRING_URL" to wireMockServer.tilgangApiBaseUrl(),
            "KAFKA_BOOTSTRAP_SERVERS" to "test",
            "AZURE_V2_ISSUER" to Azure.V2_0.getIssuer(),
            "AZURE_V2_JWKS_URI" to (wireMockServer.getAzureV2JwksUrl()),
            "AZURE_APP_CLIENT_ID" to "omsorgsdager",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_APP_TOKEN_ENDPOINT" to (wireMockServer.getAzureV2TokenUrl()),
            "OPEN_AM_ISSUER" to NaisSts.getIssuer(),
            "OPEN_AM_JWKS_URI" to (wireMockServer.getNaisStsJwksUrl()),
            "OPEN_AM_AUTHORIZED_CLIENTS" to "k9-sak"
        ),
        kafkaProducer = mockk(),
        configure = { application ->
            application.install(CORS) {
                method(HttpMethod.Options)
                method(HttpMethod.Get)
                method(HttpMethod.Post)
                method(HttpMethod.Patch)
                allowNonSimpleContentTypes = true
                header(HttpHeaders.Authorization)
                anyHost()
            }
        }
    ).build()

    internal fun start() = this
    internal fun stop() {
        embeddedPostgres.close()
        wireMockServer.stop()
    }

    internal companion object {
        internal fun ApplicationEngineEnvironmentBuilder.omsorgsdager(applicationContext: ApplicationContext) {
            connector { port = applicationContext.env.getValue("PORT").toInt() }
            module { app(applicationContext) }
        }
    }
}