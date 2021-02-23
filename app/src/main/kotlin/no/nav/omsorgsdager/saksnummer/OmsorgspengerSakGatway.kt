package no.nav.omsorgsdager.saksnummer

import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer

internal class OmsorgspengerSakGatway {
    internal suspend fun hentSaksnummer(
        identitetsnummer: Identitetsnummer,
        correlationId: CorrelationId) : OmsorgspengerSaksnummer? {
        return null // TODO
    }
}