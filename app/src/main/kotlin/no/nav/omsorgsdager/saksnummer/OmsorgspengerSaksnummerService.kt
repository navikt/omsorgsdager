package no.nav.omsorgsdager.saksnummer

import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnumer
import no.nav.omsorgsdager.parter.db.PartRepository

internal class OmsorgspengerSaksnummerService(
    private val partRepository: PartRepository,
    private val omsorgspengerSakGatway: OmsorgspengerSakGatway) {

    internal suspend fun hentSaksnummer(identitetsnummer: Identitetsnummer, correlationId: CorrelationId) : OmsorgspengerSaksnummer? {
        // 1. Sjekk 'parter' tabell
        // 2. Oppslag mot omsorgspenger-sak
        // TODO: Cache..
        return "TODO".somOmsorgspengerSaksnumer()
    }
}