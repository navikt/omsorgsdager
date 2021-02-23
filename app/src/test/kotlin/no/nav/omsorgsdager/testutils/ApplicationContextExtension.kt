package no.nav.omsorgsdager.testutils

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsJwksUrl
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.testutils.wiremock.tilgangApiBaseUrl
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import javax.sql.DataSource

internal class ApplicationContextExtension : ParameterResolver {
    private companion object {

        private val mockedEnvironment = MockedEnvironment().start()

        val env = mapOf(
            "PORT" to "8080",
            "DATABASE_HOST" to "localhost",
            "DATABASE_PORT" to "${mockedEnvironment.embeddedPostgres.port}",
            "DATABASE_DATABASE" to "postgres",
            "DATABASE_USERNAME" to "postgres",
            "DATABASE_PASSWORD" to "postgres",
            "PROXY_SCOPES" to "/.default",
            "TILGANGSSTYRING_URL" to mockedEnvironment.wireMockServer.tilgangApiBaseUrl(),
            "KAFKA_BOOTSTRAP_SERVERS" to "test",
            "AZURE_V2_ISSUER" to Azure.V2_0.getIssuer(),
            "AZURE_V2_JWKS_URI" to (mockedEnvironment.wireMockServer.getAzureV2JwksUrl()),
            "AZURE_APP_CLIENT_ID" to "omsorgsdager",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_APP_TOKEN_ENDPOINT" to (mockedEnvironment.wireMockServer.getAzureV2TokenUrl()),
            "OPEN_AM_ISSUER" to NaisSts.getIssuer(),
            "OPEN_AM_JWKS_URI" to (mockedEnvironment.wireMockServer.getNaisStsJwksUrl()),
            "OPEN_AM_AUTHORIZED_CLIENTS" to "k9-sak"
        )

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    mockedEnvironment.stop()
                }
            )
        }
    }


    private val støttedeParametre = listOf(
        ApplicationContext.Builder::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }


    private fun DataSource.cleanAndMigrate() = this.also {
        Flyway
            .configure()
            .dataSource(this)
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return ApplicationContext.Builder(
            env = env,
            onStart = {
                it.dataSource.cleanAndMigrate()
            },
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
            },
        )
    }
}
