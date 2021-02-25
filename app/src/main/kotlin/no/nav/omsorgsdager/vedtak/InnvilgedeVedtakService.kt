package no.nav.omsorgsdager.vedtak

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.GjeldendeBehandlinger
import no.nav.omsorgsdager.vedtak.infotrygd.InfotrygdInnvilgetVedtakService
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSaksnummerService
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak
import no.nav.omsorgsdager.vedtak.dto.Kilde.Companion.somKilde
import no.nav.omsorgsdager.vedtak.dto.Kilde.Companion.somKilder
import no.nav.omsorgsdager.vedtak.dto.KroniskSyktBarnInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.dto.MidlertidigAleneInnvilgetVedtak
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

        val fraInfotrygd = infotrygdInnvilgetVedtakService.hentInnvilgedeVedtak(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        )

        val omsorgspengerSaksnummer = omsorgspengerSaksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId
        )

        return when (omsorgspengerSaksnummer) {
            null -> fraInfotrygd.also { logger.info("Ingen behandligner i K9-Sak") }
            else -> fraInfotrygd.slåSammenMed(
                gjeldendeBehandlinger = behandlingService.hentAlleGjeldende(omsorgspengerSaksnummer)[Involvering.SØKER] // TODO: https://github.com/navikt/omsorgsdager/issues/41
            )
        }.also {
            logger.info(
                "FraInfotrygd[KroniskSyktBarn=${fraInfotrygd.kroniskSyktBarn.size}, MidlertidigAlene=${fraInfotrygd.midlertidigAlene.size}] " +
                "SlåttSammen[KroniskSyktBarn=${it.kroniskSyktBarn.size}, MidlertidigAlene=${it.midlertidigAlene.size}]"
            )
            cache.put(identitetsnummer to periode, it)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InnvilgedeVedtakService::class.java)

        /**
         * Slår først sammen med både innvilgede og avslåtte behandlinger fra K9-Sak
         * slik at man kan avslå en periode som er innvilget i Infotrygd.
         * Etter det er beregnet gjeldende vedtak fjernes alle vedtak som er avslått.
         */
        private fun InnvilgedeVedtak.slåSammenMed(gjeldendeBehandlinger: GjeldendeBehandlinger?) : InnvilgedeVedtak {
            if (gjeldendeBehandlinger == null) return this

            val avslåttKroniskSyktBarnKilder = gjeldendeBehandlinger.kroniskSyktBarn.filter{
                it.status == BehandlingStatus.AVSLÅTT }.map { it.k9behandlingId.somKilde() }

            val slåttSammenKroniskSyktBarn = kroniskSyktBarn.plus(gjeldendeBehandlinger.kroniskSyktBarn.map {
                KroniskSyktBarnInnvilgetVedtak(
                    tidspunkt = it.tidspunkt,
                    barn = Barn(identitetsnummer = it.barn.identitetsnummer?.toString(), fødselsdato = it.barn.fødselsdato),
                    periode = it.periode,
                    kilder = it.k9behandlingId.somKilder()
                )
            }).gjeldende().filterNot { avslåttKroniskSyktBarnKilder.contains(it.kilder.first()) }

            val avslåttMidlertidigAlene = gjeldendeBehandlinger.midlertidigAlene.filter {
                it.status == BehandlingStatus.AVSLÅTT }.map { it.k9behandlingId.somKilde() }

            val slåttSammenMidlertidigAlene = midlertidigAlene.plus(gjeldendeBehandlinger.midlertidigAlene.map {
                MidlertidigAleneInnvilgetVedtak(
                    tidspunkt = it.tidspunkt,
                    periode = it.periode,
                    kilder = it.k9behandlingId.somKilder()
                )
            }).gjeldende().filterNot { avslåttMidlertidigAlene.contains(it.kilder.first()) }

            return InnvilgedeVedtak(
                kroniskSyktBarn = slåttSammenKroniskSyktBarn,
                midlertidigAlene = slåttSammenMidlertidigAlene
            )
        }

    }
}