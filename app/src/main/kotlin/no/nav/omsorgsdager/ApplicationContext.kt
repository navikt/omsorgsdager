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
import no.nav.k9.rapid.river.csvTilSet
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.db.BehandlingRepository
import no.nav.omsorgsdager.config.*
import no.nav.omsorgsdager.config.DataSourceBuilder
import no.nav.omsorgsdager.config.Environment
import no.nav.omsorgsdager.config.hentRequiredEnv
import no.nav.omsorgsdager.parter.db.PartRepository
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSakGateway
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSaksnummerService
import no.nav.omsorgsdager.tilgangsstyring.OmsorgspengerTilgangsstyringGateway
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.tilgangsstyring.TokenResolver
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakService
import no.nav.omsorgsdager.vedtak.infotrygd.InfotrygdInnvilgetVedtakService
import no.nav.omsorgsdager.vedtak.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import java.net.URI
import javax.sql.DataSource

internal class ApplicationContext(
    internal val env: Environment,
    internal val dataSource: DataSource,
    internal val healthService: HealthService,
    internal val omsorgspengerTilgangsstyringGateway: OmsorgspengerTilgangsstyringGateway,
    internal val tokenResolver: TokenResolver,
    internal val tilgangsstyring: Tilgangsstyring,
    internal val behandlingRepository: BehandlingRepository,
    internal val behandlingService: BehandlingService,
    internal val partRepository: PartRepository,
    internal val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway,
    internal val infotrygdInnvilgetVedtakService: InfotrygdInnvilgetVedtakService,
    internal val omsorgspengerSakGateway: OmsorgspengerSakGateway,
    internal val omsorgspengerSaksnummerService: OmsorgspengerSaksnummerService,
    internal val innvilgedeVedtakService: InnvilgedeVedtakService,
    internal val configure: (application: Application) -> Unit,
    private val onStart: (applicationContext: ApplicationContext) -> Unit,
    private val onStop: (applicationContext: ApplicationContext) -> Unit) {

    internal fun start() = onStart(this)
    internal fun stop() = onStop(this)

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var httpClient: HttpClient? = null,
        var accessTokenClient: AccessTokenClient? = null,
        var omsorgspengerTilgangsstyringGateway: OmsorgspengerTilgangsstyringGateway? = null,
        var tokenResolver: TokenResolver? = null,
        var tilgangsstyring: Tilgangsstyring? = null,
        var behandlingRepository: BehandlingRepository? = null,
        var behandlingService: BehandlingService? = null,
        var partRepository: PartRepository? = null,
        var omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway? = null,
        var infotrygdInnvilgetVedtakService: InfotrygdInnvilgetVedtakService? = null,
        var omsorgspengerSakGateway: OmsorgspengerSakGateway? = null,
        var omsorgspengerSaksnummerService: OmsorgspengerSaksnummerService? = null,
        var innvilgedeVedtakService: InnvilgedeVedtakService? = null,
        var configure: (application: Application) -> Unit = {},
        var onStart: (applicationContext: ApplicationContext) -> Unit = {}, // TODO: Migrate
        var onStop: (applicationContext: ApplicationContext) -> Unit = {}) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            }
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
            val benyttetBehandlingRepository = behandlingRepository ?: BehandlingRepository(
                dataSource = benyttetDataSource
            )
            val benyttetPartRepository = partRepository ?: PartRepository(
                dataSource = benyttetDataSource
            )

            val benyttetAccessTokenClient = accessTokenClient?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_APP_TOKEN_ENDPOINT"))
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

            val benyttetBehandlingService = behandlingService ?: BehandlingService(
                behandlingRepository = benyttetBehandlingRepository,
                partRepository = benyttetPartRepository,
                dataSource = benyttetDataSource
            )

            val benyttetOmsorgspengerInfotrygdRammevedtakGateway = omsorgspengerInfotrygdRammevedtakGateway ?: OmsorgspengerInfotrygdRammevedtakGateway(
                accessTokenClient = benyttetAccessTokenClient,
                hentRammevedtakFraInfotrygdScopes = benyttetEnv.hentRequiredEnv("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES").csvTilSet(),
                omsorgspengerInfotrygdRammevedtakBaseUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL"))
            )

            val benyttetInfotrygdInnvilgetVedtakService = infotrygdInnvilgetVedtakService ?: InfotrygdInnvilgetVedtakService(
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
            )
            val benyttetOmsorgspengerSakGateway = omsorgspengerSakGateway ?: OmsorgspengerSakGateway(
                accessTokenClient = benyttetAccessTokenClient,
                hentSaksnummerFraOmsorgspengerSakScopes = benyttetEnv.hentRequiredEnv("HENT_SAKSNUMMER_FRA_OMSORGSPENGER_SAK_SCOPES").csvTilSet(),
                omsorgspengerSakUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_SAK_BASE_URL"))
            )
            val benyttetOmsorgspengerSaksnummerService = omsorgspengerSaksnummerService ?: OmsorgspengerSaksnummerService(
                omsorgspengerSakGateway = benyttetOmsorgspengerSakGateway,
                partRepository = benyttetPartRepository
            )
            val benyttetInnvilgedeVedtakService = innvilgedeVedtakService ?: InnvilgedeVedtakService(
                behandlingService = benyttetBehandlingService,
                omsorgspengerSaksnummerService = benyttetOmsorgspengerSaksnummerService,
                infotrygdInnvilgetVedtakService = benyttetInfotrygdInnvilgetVedtakService,
                hentBehandlinger = benyttetEnv.hentOptionalEnv("HENT_BEHANDLINGER") == "enabled"
            )

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                healthService = HealthService(
                    healthChecks = setOf(
                        benyttetOmsorgspengerTilgangsstyringGateway,
                        benyttetOmsorgspengerInfotrygdRammevedtakGateway,
                        benyttetOmsorgspengerSakGateway
                    )
                ),
                omsorgspengerTilgangsstyringGateway = benyttetOmsorgspengerTilgangsstyringGateway,
                tokenResolver = benyttetTokenResolver,
                tilgangsstyring = benyttetTilgangsstyring,
                configure = configure,
                behandlingRepository = benyttetBehandlingRepository,
                behandlingService = benyttetBehandlingService,
                omsorgspengerSaksnummerService = benyttetOmsorgspengerSaksnummerService,
                omsorgspengerSakGateway = benyttetOmsorgspengerSakGateway,
                innvilgedeVedtakService = benyttetInnvilgedeVedtakService,
                infotrygdInnvilgetVedtakService = benyttetInfotrygdInnvilgetVedtakService,
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway,
                partRepository = benyttetPartRepository,
                onStart = onStart,
                onStop = onStop
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
