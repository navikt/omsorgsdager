package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.json.*
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.omsorgsdager.config.DataSourceBuilder
import no.nav.omsorgsdager.config.Environment
import no.nav.omsorgsdager.config.KafkaBuilder.kafkaProducer
import no.nav.omsorgsdager.config.ServiceUser
import no.nav.omsorgsdager.config.hentRequiredEnv
import no.nav.omsorgsdager.config.migrate
import no.nav.omsorgsdager.config.readServiceUserCredentials
import no.nav.omsorgsdager.pdl.PdlClient
import org.apache.kafka.clients.producer.KafkaProducer
import java.net.URI
import javax.sql.DataSource

internal class ApplicationContext(
    internal val env: Environment,
    internal val dataSource: DataSource,
    internal val healthService: HealthService,
    internal val tilgangsstyringRestClient: TilgangsstyringRestClient,
    internal val kafkaProducer: KafkaProducer<String, String>
) {

    internal fun start() {
        dataSource.migrate()
    }

    internal fun stop() {}

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var serviceUser: ServiceUser? = null,
        var httpClient: HttpClient? = null,
        var accessTokenClient: AccessTokenClient? = null,
        var pdlClient: PdlClient? = null,
        var tilgangsstyringRestClient: TilgangsstyringRestClient? = null,
        var kafkaProducer: KafkaProducer<String, String>? = null,
        ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            }
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
            val benyttetServiceUser = serviceUser ?: readServiceUserCredentials()
            val benyttetAccessTokenClient = accessTokenClient?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_APP_TOKEN_ENDPOINT"))
            )
            val benyttetPdlClient = pdlClient ?: PdlClient(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient,
                objectMapper = objectMapper
            )
            val benyttetTilgangsstyringRestClient = tilgangsstyringRestClient ?: TilgangsstyringRestClient(
                httpClient = benyttetHttpClient,
                env = benyttetEnv
            )
            val benyttetKafkaProducer =  kafkaProducer ?: benyttetEnv.kafkaProducer()

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                healthService = HealthService(
                    healthChecks = setOf(
                        benyttetPdlClient,
                        benyttetTilgangsstyringRestClient
                    )
                ),
                tilgangsstyringRestClient = benyttetTilgangsstyringRestClient,
                kafkaProducer = benyttetKafkaProducer
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
