package no.nav.omsorgsdager.vedtak.infotrygd

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.tid.Periode

internal class OmsorgspengerInfotrygdRammevedtakGateway {

    internal suspend fun hentInnvilgedeVedtak(identitetsnummer: Identitetsnummer, periode: Periode) : List<InfotrygdInnvilgetVedtak> {
        return emptyList() // TODO:
    }
}