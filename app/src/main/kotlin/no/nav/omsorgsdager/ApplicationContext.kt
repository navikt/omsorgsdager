package no.nav.omsorgsdager

import io.ktor.application.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.db.BehandlingRepository
import no.nav.omsorgsdager.parter.db.PartRepository
import no.nav.omsorgsdager.person.PersonInfoGateway
import no.nav.omsorgsdager.person.pdl.PdlPersonInfoGateway
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
    internal val healthChecks: Set<HealthCheck>,
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
    internal val personInfoGatway: PersonInfoGateway,
    internal val configure: (application: Application) -> Unit,
    private val onStart: (applicationContext: ApplicationContext) -> Unit,
    private val onStop: (applicationContext: ApplicationContext) -> Unit) {

    internal fun start() = onStart(this)
    internal fun stop() = onStop(this)
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
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
        var personInfoGatway: PersonInfoGateway? = null,
        var configure: (application: Application) -> Unit = {},
        var onStart: (applicationContext: ApplicationContext) -> Unit = {
            it.dataSource.migrate()
        },
        var onStop: (applicationContext: ApplicationContext) -> Unit = {}) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
            val benyttetBehandlingRepository = behandlingRepository ?: BehandlingRepository(
                dataSource = benyttetDataSource
            )
            val benyttetPartRepository = partRepository ?: PartRepository(
                dataSource = benyttetDataSource
            )

            val benyttetAccessTokenClient = accessTokenClient ?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")),
                authenticationMode = ClientSecretAccessTokenClient.AuthenticationMode.POST
            )

            val benyttetOmsorgspengerTilgangsstyringGateway = omsorgspengerTilgangsstyringGateway ?: OmsorgspengerTilgangsstyringGateway(
                baseUri = URI.create(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_TILGANGSSTYRING_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("OMSORGSPENGER_TILGANGSSTYRING_SCOPES").csvTilSet(),
                accessTokenClient = benyttetAccessTokenClient
            )

            val benyttetTokenResolver = tokenResolver ?: TokenResolver(
                azureIssuers = setOf(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_ISSUER")),
                openAmIssuers = setOf(benyttetEnv.hentRequiredEnv("OPEN_AM_ISSUER")),
                openAmAuthorizedClients = benyttetEnv.hentRequiredEnv("OPEN_AM_AUTHORIZED_CLIENTS").csvTilSet()
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
                scopes = benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_SCOPES").csvTilSet(),
                omsorgspengerInfotrygdRammevedtakBaseUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL"))
            )

            val benyttetInfotrygdInnvilgetVedtakService = infotrygdInnvilgetVedtakService ?: InfotrygdInnvilgetVedtakService(
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
            )
            val benyttetOmsorgspengerSakGateway = omsorgspengerSakGateway ?: OmsorgspengerSakGateway(
                accessTokenClient = benyttetAccessTokenClient,
                scopes = benyttetEnv.hentRequiredEnv("OMSORGSPENGER_SAK_SCOPES").csvTilSet(),
                omsorgspengerSakUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_SAK_BASE_URL"))
            )
            val benyttetOmsorgspengerSaksnummerService = omsorgspengerSaksnummerService ?: OmsorgspengerSaksnummerService(
                omsorgspengerSakGateway = benyttetOmsorgspengerSakGateway,
                partRepository = benyttetPartRepository
            )
            val benyttetInnvilgedeVedtakService = innvilgedeVedtakService ?: InnvilgedeVedtakService(
                behandlingService = benyttetBehandlingService,
                omsorgspengerSaksnummerService = benyttetOmsorgspengerSaksnummerService,
                infotrygdInnvilgetVedtakService = benyttetInfotrygdInnvilgetVedtakService
            )

            val benyttetPersonInfoGateway = personInfoGatway ?: PdlPersonInfoGateway(
                baseUri = URI(benyttetEnv.hentRequiredEnv("PDL_BASE_URL")),
                accessTokenClient = benyttetAccessTokenClient,
                scopes = benyttetEnv.hentRequiredEnv("PDL_SCOPES").csvTilSet()
            )

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                healthChecks = setOf(
                    benyttetOmsorgspengerTilgangsstyringGateway,
                    benyttetOmsorgspengerInfotrygdRammevedtakGateway,
                    benyttetOmsorgspengerSakGateway,
                    benyttetPersonInfoGateway
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
                personInfoGatway = benyttetPersonInfoGateway,
                onStart = onStart,
                onStop = onStop
            )
        }
    }
}
