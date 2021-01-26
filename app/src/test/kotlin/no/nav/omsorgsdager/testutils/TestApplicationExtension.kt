package no.nav.omsorgsdager.testutils

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.app
import no.nav.omsorgsdager.config.ServiceUser
import no.nav.omsorgsdager.testutils.wiremock.pdlApiBaseUrl
import no.nav.omsorgsdager.testutils.wiremock.stubPdlApi
import no.nav.omsorgsdager.testutils.wiremock.stubTilgangApi
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
                "TILGANGSSTYRING_URL" to "test/.default",
                "AZURE_V2_ISSUER" to Azure.V2_0.getIssuer(),
                "AZURE_V2_JWKS_URI" to (wireMockServer.getAzureV2JwksUrl()),
                "AZURE_APP_CLIENT_ID" to "omsorgsdager"
            ),
            serviceUser = ServiceUser("foo", "bar"),
            accessTokenClient = ClientSecretAccessTokenClient(
                clientId = "omsorgsdager",
                clientSecret = "azureSecret",
                tokenEndpoint = URI(wireMockServer.getAzureV2TokenUrl()),
            )
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
                    embeddedPostgres.postgresDatabase.connection.close()
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

@KtorExperimentalAPI
private fun getConfig(config: MutableMap<String, String>): ApplicationConfig {
    config.medAppConfig(8083)
    val fileConfig = ConfigFactory.load()
    val testConfig = ConfigFactory.parseMap(config)
    val mergedConfig = testConfig.withFallback(fileConfig)
    return HoconApplicationConfig(mergedConfig)
}

internal fun MutableMap<String, String>.medAppConfig(port: Int) = also {
    it.putIfAbsent("ktor.deployment.port", "$port")
}