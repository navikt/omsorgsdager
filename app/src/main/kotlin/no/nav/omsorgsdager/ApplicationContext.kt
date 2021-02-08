package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.omsorgsdager.config.*
import no.nav.omsorgsdager.config.DataSourceBuilder
import no.nav.omsorgsdager.config.Environment
import no.nav.omsorgsdager.config.KafkaBuilder.kafkaProducer
import no.nav.omsorgsdager.config.hentRequiredEnv
import no.nav.omsorgsdager.config.migrate
import no.nav.omsorgsdager.kronisksyktbarn.InMemoryKroniskSyktBarnRespository
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnRepository
import no.nav.omsorgsdager.pdl.PdlClient
import no.nav.omsorgsdager.tilgangsstyring.OmsorgspengerTilgangsstyringGateway
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.tilgangsstyring.TokenResolver
import org.apache.kafka.clients.producer.KafkaProducer
import java.net.URI
import javax.sql.DataSource

internal class ApplicationContext(
    internal val env: Environment,
    internal val dataSource: DataSource,
    internal val healthService: HealthService,
    internal val omsorgspengerTilgangsstyringGateway: OmsorgspengerTilgangsstyringGateway,
    internal val tokenResolver: TokenResolver,
    internal val tilgangsstyring: Tilgangsstyring,
    internal val kafkaProducer: KafkaProducer<String, String>,
    internal val kroniskSyktBarnRepository: KroniskSyktBarnRepository,
    internal val configure: (application: Application) -> Unit) {

    internal fun start() {
        dataSource.migrate()
    }

    internal fun stop() {}

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var httpClient: HttpClient? = null,
        var accessTokenClient: AccessTokenClient? = null,
        var pdlClient: PdlClient? = null,
        var omsorgspengerTilgangsstyringGateway: OmsorgspengerTilgangsstyringGateway? = null,
        var tokenResolver: TokenResolver? = null,
        var tilgangsstyring: Tilgangsstyring? = null,
        var kafkaProducer: KafkaProducer<String, String>? = null,
        var kroniskSyktBarnRepository: KroniskSyktBarnRepository? = null,
        var configure: (application: Application) -> Unit = {}) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            }
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
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
            val benyttetOmsorgspengerTilgangsstyringGateway = omsorgspengerTilgangsstyringGateway ?: OmsorgspengerTilgangsstyringGateway(
                httpClient = benyttetHttpClient,
                omsorgspengerTilgangsstyringUri = URI.create(benyttetEnv.hentRequiredEnv("TILGANGSSTYRING_URL"))
            )

            val benyttetTokenResolver = tokenResolver ?: TokenResolver(
                azureIssuers = setOf(benyttetEnv.hentRequiredEnv("AZURE_V2_ISSUER")),
                openAmIssuers = setOf(benyttetEnv.hentRequiredEnv("OPEN_AM_ISSUER")),
                openAmAuthorizedClients = benyttetEnv.hentRequiredEnv("OPEN_AM_AUTHORIZED_CLIENTS").csv()
            )

            val benyttetTilgangsstyring = tilgangsstyring ?: Tilgangsstyring(
                tokenResolver = benyttetTokenResolver,
                omsorgspengerTilgangsstyringGateway = benyttetOmsorgspengerTilgangsstyringGateway
            )

            val benyttetKafkaProducer = kafkaProducer ?: benyttetEnv.kafkaProducer()

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                healthService = HealthService(
                    healthChecks = setOf(
                        benyttetPdlClient,
                        benyttetOmsorgspengerTilgangsstyringGateway
                    )
                ),
                kafkaProducer = benyttetKafkaProducer,
                omsorgspengerTilgangsstyringGateway = benyttetOmsorgspengerTilgangsstyringGateway,
                tokenResolver = benyttetTokenResolver,
                tilgangsstyring = benyttetTilgangsstyring,
                kroniskSyktBarnRepository = kroniskSyktBarnRepository ?: InMemoryKroniskSyktBarnRespository(),
                configure = configure
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
