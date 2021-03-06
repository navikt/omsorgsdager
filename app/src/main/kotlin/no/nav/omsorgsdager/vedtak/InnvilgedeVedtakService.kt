package no.nav.omsorgsdager.vedtak

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.GjeldendeBehandlinger
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnBehandling
import no.nav.omsorgsdager.midlertidigalene.MidlertidigAleneBehandling
import no.nav.omsorgsdager.vedtak.infotrygd.InfotrygdInnvilgetVedtakService
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSaksnummerService
import no.nav.omsorgsdager.tid.Gjeldende.flatten
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Gjeldende.gjeldendePer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.Barn.Companion.sammenlignPå
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
            null -> fraInfotrygd.slåSammenMed(GjeldendeBehandlinger()).also { logger.info("Ingen behandligner i K9-Sak") }
            else -> fraInfotrygd.slåSammenMed(
                gjeldendeBehandlinger = behandlingService.hentAlleGjeldende(
                    saksnummer = omsorgspengerSaksnummer,
                    periode = periode
                )[Involvering.SØKER]?: GjeldendeBehandlinger()
            )
        }.also {
            logger.info(
                "FraInfotrygd[KroniskSyktBarn=${fraInfotrygd.kroniskSyktBarn.size}, MidlertidigAlene=${fraInfotrygd.midlertidigAlene.size}] " +
                "SlåttSammen[KroniskSyktBarn=${it.kroniskSyktBarn.size}, MidlertidigAlene=${it.midlertidigAlene.size}]"
            )
            cache.put(identitetsnummer to periode, it)
        }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(InnvilgedeVedtakService::class.java)

        private fun KroniskSyktBarnBehandling.somBarn() = Barn(identitetsnummer = barn.identitetsnummer, omsorgspengerSaksnummer = barn.omsorgspengerSaksnummer, fødselsdato = barn.fødselsdato)

        // Gjøres etter at vi har funnet `gjeldende` som sorterer vedtakene descending på tidspunkt, derfor tar vi den første (som har nyest tidspunkt)
        private fun List<Barn>.medMestInfo() =
            firstOrNull { it.omsorgspengerSaksnummer != null }
            ?:firstOrNull { it.identitetsnummer != null }
            ?:first()

        /**
         * Regner også med avslåtte behandlinger slik at de overskriver eventuelle innvilgede perioder i Infotrygd.
         */
        private fun List<KroniskSyktBarnInnvilgetVedtak>.slåSammenKroniskSykeBarn(kroniskSyktBarnBehandlinger: List<KroniskSyktBarnBehandling>) : List<KroniskSyktBarnInnvilgetVedtak> {
            // Finner først alle kilder for avslåtte behandlinger slik at de kan filtreres bort i det ferdige resultatet
            val avslåtteKilder = kroniskSyktBarnBehandlinger.filter{
                it.status == BehandlingStatus.AVSLÅTT }.map { it.k9behandlingId.somKilde() }

            // Slår sammen alle vedtakene vi sammenligner
            val alle = plus(kroniskSyktBarnBehandlinger.map {
                KroniskSyktBarnInnvilgetVedtak(
                    tidspunkt = it.tidspunkt,
                    barn = it.somBarn(),
                    periode = it.periode,
                    kilder = it.k9behandlingId.somKilder()
                )
            })

            // Finner ut hvordan vi kan sammenligne denne samlingen med barn
            val sammenlignBarnPå = alle.map { it.barn }.sammenlignPå()

            // 1. Lager en kopi av alle vedtak med sammenligningsmåten utledet over
            // 2. Plukker ut barnet med mest info på seg og setter det på alle vedtak
            // 3. Fjerner til slutt alle avslåtte behandlinger slik at vi kun sitter igjen med innvilgede perioder.
            return alle.map { it.copy(enPer = sammenlignBarnPå(it.barn)) }.gjeldendePer().mapValues {
                val barnMedMestInfo = it.value.map { vedtak -> vedtak.barn }.medMestInfo()
                it.value.map { vedtak -> vedtak.copy(barn = barnMedMestInfo) }
            }.flatten().filterNot { avslåtteKilder.contains(it.kilder.firstOrNull()) }
        }

        private fun List<MidlertidigAleneInnvilgetVedtak>.slåSammenMidlertidigAlene(midlertidigAlenebehandlinger: List<MidlertidigAleneBehandling>) : List<MidlertidigAleneInnvilgetVedtak> {
            // Finner først alle kilder for avslåtte behandlinger slik at de kan filtreres bort i det ferdige resultatet
            val avslåtteKilder = midlertidigAlenebehandlinger.filter {
                it.status == BehandlingStatus.AVSLÅTT }.map { it.k9behandlingId.somKilde() }

            // Fjerner til slutt alle avslåtte behandlinger slik at vi kun sitter igjen med innvilgede perioder.
            return plus(midlertidigAlenebehandlinger.map {
                MidlertidigAleneInnvilgetVedtak(
                    tidspunkt = it.tidspunkt,
                    periode = it.periode,
                    kilder = it.k9behandlingId.somKilder()
                )
            }).gjeldende().filterNot { avslåtteKilder.contains(it.kilder.first()) }
        }

        internal fun InnvilgedeVedtak.slåSammenMed(gjeldendeBehandlinger: GjeldendeBehandlinger) = InnvilgedeVedtak(
            kroniskSyktBarn = kroniskSyktBarn.slåSammenKroniskSykeBarn(gjeldendeBehandlinger.kroniskSyktBarn),
            midlertidigAlene = midlertidigAlene.slåSammenMidlertidigAlene(gjeldendeBehandlinger.midlertidigAlene)
        )
    }
}