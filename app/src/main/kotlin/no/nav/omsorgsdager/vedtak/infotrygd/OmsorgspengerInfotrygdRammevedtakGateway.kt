package no.nav.omsorgsdager.vedtak.infotrygd

import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.tid.Periode

internal class OmsorgspengerInfotrygdRammevedtakGateway {

    internal suspend fun hentInnvilgedeVedtak(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) : List<InfotrygdInnvilgetVedtak> {
        return emptyList() // TODO:
    }
}