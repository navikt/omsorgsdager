package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.saksnummer.OmsorgspengerSaksnummerService
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak
import org.slf4j.LoggerFactory

internal class InnvilgedeVedtakService(
    private val behandlingService: BehandlingService,
    private val omsorgspengerSaksnummerService: OmsorgspengerSaksnummerService) {

    internal suspend fun hentInnvilgedeVedtak(identitetsnummer: Identitetsnummer, periode: Periode) : InnvilgedeVedtak {
        val omsorgspengerSaksnummer = omsorgspengerSaksnummerService.hentSaksnummer(identitetsnummer)

        val fraInfotrygd = InnvilgedeVedtak(kroniskSyktBarn = emptyList(), midlertidigAlene = emptyList()) // TODO

        val fraK9Sak = when (omsorgspengerSaksnummer) {
            null -> InnvilgedeVedtak.ingenInnvilgedeVedtak().also {
                logger.info("Personen har ikke et omsorgspenger saksnummer og dermed heller ingen behandlinger i K9-sak.")
            }
            else -> InnvilgedeVedtak.gjeldendeBehandlingerSomInnvilgedeVedtak(
                gjeldendeBehandlinger = behandlingService.hentAlleGjeldende(omsorgspengerSaksnummer)[Involvering.SØKER]
            )
        }

        logger.info(
            "Infotrygd[KroniskSyktBarn=${fraInfotrygd.kroniskSyktBarn.size}, MidlertidigAlene=${fraInfotrygd.midlertidigAlene.size}]" +
                "K9-sak[KroniskSyktBarn=${fraK9Sak.kroniskSyktBarn.size}, MidlertidigAlene=${fraK9Sak.midlertidigAlene.size}]"
        )

        return when {
            fraInfotrygd.isEmpty -> fraK9Sak
            fraK9Sak.isEmpty -> fraInfotrygd
            else -> InnvilgedeVedtak(
                kroniskSyktBarn = fraInfotrygd.kroniskSyktBarn.plus(fraK9Sak.kroniskSyktBarn).gjeldende(),
                midlertidigAlene = fraInfotrygd.midlertidigAlene.plus(fraK9Sak.midlertidigAlene).gjeldende()
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InnvilgedeVedtakService::class.java)
    }
}