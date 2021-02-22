package no.nav.omsorgsdager.saksnummer

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnumer

internal class OmsorgspengerSaksnummerService {

    internal suspend fun hentSaksnummer(identitetsnummer: Identitetsnummer) : OmsorgspengerSaksnummer? {
        // 1. Sjekk 'parter' tabell
        // 2. Oppslag mot omsorgspenger-sak
        return "TODO".somOmsorgspengerSaksnumer()
    }
}