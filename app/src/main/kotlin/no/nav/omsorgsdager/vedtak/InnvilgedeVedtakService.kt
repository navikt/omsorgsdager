package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnumer
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak

internal class InnvilgedeVedtakService(
    private val behandlingService: BehandlingService, ) {

    internal fun hentInnvilgedeVedtak(identitetsnummer: Identitetsnummer, periode: Periode) : InnvilgedeVedtak {
        val omsorgspengerSaksnummer = "TODO".somOmsorgspengerSaksnumer()

        val fraInfotrygd = InnvilgedeVedtak(kroniskSyktBarn = emptyList(), midlertidigAlene = emptyList()) // TODO

        val fraK9Sak = InnvilgedeVedtak.gjeldendeBehandlingerSomInnvilgedeVedtak(
            gjeldendeBehandlinger = behandlingService.hentAlleGjeldende(omsorgspengerSaksnummer)[Involvering.SØKER]
        )

        return InnvilgedeVedtak(
            kroniskSyktBarn = fraInfotrygd.kroniskSyktBarn.plus(fraK9Sak.kroniskSyktBarn).gjeldende(),
            midlertidigAlene = fraInfotrygd.midlertidigAlene.plus(fraK9Sak.midlertidigAlene).gjeldende()
        )
    }
}