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
import no.nav.omsorgsdager.testutils.wiremock.infotrygdRammevedtakBaseUrl
import no.nav.omsorgsdager.testutils.wiremock.omsorgspengerSakBaseUrl
import no.nav.omsorgsdager.testutils.wiremock.tilgangApiBaseUrl
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import javax.sql.DataSource

internal class ApplicationContextExtension : ParameterResolver {
    internal companion object {

        internal fun ApplicationContext.Builder.buildStarted() = build().also { it.start() }

        private val mockedEnvironment = MockedEnvironment().start()

        private val env = mapOf(
            "DATABASE_HOST" to "localhost",
            "DATABASE_PORT" to "${mockedEnvironment.embeddedPostgres.port}",
            "DATABASE_DATABASE" to "postgres",
            "DATABASE_USERNAME" to "postgres",
            "DATABASE_PASSWORD" to "postgres",
            "TILGANGSSTYRING_URL" to mockedEnvironment.wireMockServer.tilgangApiBaseUrl(),
            "AZURE_V2_ISSUER" to Azure.V2_0.getIssuer(),
            "AZURE_V2_JWKS_URI" to (mockedEnvironment.wireMockServer.getAzureV2JwksUrl()),
            "AZURE_APP_CLIENT_ID" to "omsorgsdager",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_APP_TOKEN_ENDPOINT" to (mockedEnvironment.wireMockServer.getAzureV2TokenUrl()),
            "OPEN_AM_ISSUER" to NaisSts.getIssuer(),
            "OPEN_AM_JWKS_URI" to (mockedEnvironment.wireMockServer.getNaisStsJwksUrl()),
            "OPEN_AM_AUTHORIZED_CLIENTS" to "k9-sak",
            "HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES" to "/.default",
            "OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL" to mockedEnvironment.wireMockServer.infotrygdRammevedtakBaseUrl(),
            "OMSORGSPENGER_SAK_BASE_URL" to mockedEnvironment.wireMockServer.omsorgspengerSakBaseUrl(),
            "HENT_SAKSNUMMER_FRA_OMSORGSPENGER_SAK_SCOPES" to "/.default"
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
            }
        )
    }
}
