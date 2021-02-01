package no.nav.omsorgsdager.testutils

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.mockk
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import no.nav.helse.dusseldorf.testsupport.wiremock.*
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.app
import no.nav.omsorgsdager.testutils.wiremock.pdlApiBaseUrl
import no.nav.omsorgsdager.testutils.wiremock.stubPdlApi
import no.nav.omsorgsdager.testutils.wiremock.stubTilgangApi
import no.nav.omsorgsdager.testutils.wiremock.tilgangApiBaseUrl
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.net.URI
import java.nio.file.Files.createTempDirectory
import java.util.concurrent.TimeUnit

internal class TestApplicationExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {

        private fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(tempDir)
            .setDataDirectory(tempDir.resolve("datadir"))
            .start()

        private val embeddedPostgres = embeddedPostgress(createTempDirectory("tmp_postgres").toFile())
        private val wireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubTilgangApi()
            .stubPdlApi()

        val applicationContext = ApplicationContext.Builder(
            env = mapOf(
                "DATABASE_HOST" to "localhost",
                "DATABASE_PORT" to "${embeddedPostgres.port}",
                "DATABASE_DATABASE" to "postgres",
                "DATABASE_USERNAME" to "postgres",
                "DATABASE_PASSWORD" to "postgres",
                "PDL_BASE_URL" to wireMockServer.pdlApiBaseUrl(),
                "STS_TOKEN_ENDPOINT" to wireMockServer.getNaisStsTokenUrl(),
                "PROXY_SCOPES" to "/.default",
                "TILGANGSSTYRING_URL" to wireMockServer.tilgangApiBaseUrl(),
                "KAFKA_BOOTSTRAP_SERVERS" to "test",
                "AZURE_V2_ISSUER" to Azure.V2_0.getIssuer(),
                "AZURE_V2_JWKS_URI" to (wireMockServer.getAzureV2JwksUrl()),
                "AZURE_APP_CLIENT_ID" to "omsorgsdager",
                "OPEN_AM_ISSUER" to NaisSts.getIssuer(),
                "OPEN_AM_JWKS_URI" to (wireMockServer.getNaisStsJwksUrl()),
                "OPEN_AM_AUTHORIZED_CLIENTS" to "k9-sak"
            ),
            accessTokenClient = ClientSecretAccessTokenClient( // TODO: Why?
                clientId = "omsorgsdager",
                clientSecret = "azureSecret",
                tokenEndpoint = URI(wireMockServer.getAzureV2TokenUrl()),
            ),
            kafkaProducer = mockk()
        ).build()

        @KtorExperimentalAPI
        internal val testApplicationEngine = TestApplicationEngine(
            environment = createTestEnvironment {
                config = HoconApplicationConfig(ConfigFactory.load().withoutPath("ktor.application.modules"))
                module { app(applicationContext) }
            }
        )

        init {
            testApplicationEngine.start(wait = true)
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    testApplicationEngine.stop(10, 60, TimeUnit.SECONDS)
                    embeddedPostgres.close()
                    wireMockServer.stop()
                }
            )
        }
    }

    private val støttedeParametre = listOf(
        TestApplicationEngine::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    @KtorExperimentalAPI
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return testApplicationEngine
    }
}