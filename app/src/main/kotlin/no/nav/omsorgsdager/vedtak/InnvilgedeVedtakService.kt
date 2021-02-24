package no.nav.omsorgsdager.vedtak

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.vedtak.infotrygd.InfotrygdInnvilgetVedtakService
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSaksnummerService
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak
import org.slf4j.LoggerFactory
import java.time.Duration

internal class InnvilgedeVedtakService(
    private val behandlingService: BehandlingService,
    private val omsorgspengerSaksnummerService: OmsorgspengerSaksnummerService,
    private val infotrygdInnvilgetVedtakService: InfotrygdInnvilgetVedtakService) {

    private val cache: Cache<Pair<Identitetsnummer, Periode>, InnvilgedeVedtak> =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(15))
            .maximumSize(200)
            .build()

    internal suspend fun hentInnvilgedeVedtak(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) : InnvilgedeVedtak {
        val fraCache = cache.getIfPresent(identitetsnummer to periode)

        if (fraCache != null) {
            return fraCache
        }

        val omsorgspengerSaksnummer = omsorgspengerSaksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId
        )

        val fraInfotrygd = infotrygdInnvilgetVedtakService.hentInnvilgedeVedtak(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        )

        val fraK9Sak = when (omsorgspengerSaksnummer) {
            null -> InnvilgedeVedtak.ingenInnvilgedeVedtak().also {
                logger.info("Personen har ikke et omsorgspenger saksnummer og dermed heller ingen behandlinger i K9-Sak.")
            }
            else -> {
                val gjeldendeBehandlingerSomSøker = behandlingService.hentAlleGjeldende(omsorgspengerSaksnummer)[Involvering.SØKER] // TODO: Sende inn periode her
                InnvilgedeVedtak.gjeldendeBehandlingerSomInnvilgedeVedtak(gjeldendeBehandlinger = gjeldendeBehandlingerSomSøker)
            }
        }

        logger.info(
            "Infotrygd[KroniskSyktBarn=${fraInfotrygd.kroniskSyktBarn.size}, MidlertidigAlene=${fraInfotrygd.midlertidigAlene.size}] " +
            "K9-Sak[KroniskSyktBarn=${fraK9Sak.kroniskSyktBarn.size}, MidlertidigAlene=${fraK9Sak.midlertidigAlene.size}]"
        )

        return when {
            fraInfotrygd.isEmpty -> fraK9Sak
            fraK9Sak.isEmpty -> fraInfotrygd
            else -> InnvilgedeVedtak(
                kroniskSyktBarn = fraInfotrygd.kroniskSyktBarn.plus(fraK9Sak.kroniskSyktBarn).gjeldende(),
                midlertidigAlene = fraInfotrygd.midlertidigAlene.plus(fraK9Sak.midlertidigAlene).gjeldende()
            )
        }.also {
            cache.put(identitetsnummer to periode, it)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InnvilgedeVedtakService::class.java)
    }
}