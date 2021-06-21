package no.nav.omsorgsdager.testutils

import com.github.tomakehurst.wiremock.WireMockServer
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
import no.nav.omsorgsdager.testutils.wiremock.pdlBaseUrl
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
            "OMSORGSPENGER_TILGANGSSTYRING_BASE_URL" to mockedEnvironment.wireMockServer.tilgangApiBaseUrl(),
            "OMSORGSPENGER_TILGANGSSTYRING_SCOPES" to "omsorgspenger-tilgangsstyring/.default",
            "AZURE_OPENID_CONFIG_ISSUER" to Azure.V2_0.getIssuer(),
            "AZURE_OPENID_CONFIG_JWKS_URI" to (mockedEnvironment.wireMockServer.getAzureV2JwksUrl()),
            "AZURE_APP_CLIENT_ID" to "omsorgsdager",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to (mockedEnvironment.wireMockServer.getAzureV2TokenUrl()),
            "OPEN_AM_ISSUER" to NaisSts.getIssuer(),
            "OPEN_AM_JWKS_URI" to (mockedEnvironment.wireMockServer.getNaisStsJwksUrl()),
            "OPEN_AM_AUTHORIZED_CLIENTS" to "k9-sak",
            "OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL" to mockedEnvironment.wireMockServer.infotrygdRammevedtakBaseUrl(),
            "OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_SCOPES" to "omsorgspenger-infotrygd-rammevedtak/.default",
            "OMSORGSPENGER_RAMMEMELDINGER_BASE_URL" to mockedEnvironment.wireMockServer.infotrygdRammevedtakBaseUrl(), // TODO: Bytt & legg i nais
            "OMSORGSPENGER_RAMMEMELDINGER_SCOPES" to "omsorgspenger-rammemeldinger/.default",
            "OMSORGSPENGER_SAK_BASE_URL" to mockedEnvironment.wireMockServer.omsorgspengerSakBaseUrl(),
            "OMSORGSPENGER_SAK_SCOPES" to "omsorgspenger-sak/.default",
            "PDL_BASE_URL" to mockedEnvironment.wireMockServer.pdlBaseUrl(),
            "PDL_SCOPES" to "pdl/.default"
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
        ApplicationContext.Builder::class.java,
        WireMockServer::class.java
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
        return when (parameterContext.parameter.type) {
            WireMockServer::class.java -> mockedEnvironment.wireMockServer
            else -> ApplicationContext.Builder(
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
}
