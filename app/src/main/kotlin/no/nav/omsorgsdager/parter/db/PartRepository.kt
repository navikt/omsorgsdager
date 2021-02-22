package no.nav.omsorgsdager.parter.db

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.parter.Involvering

internal class PartRepository {
    internal fun hentParter(behandlingIder: List<BehandlingId>) : List<DbPart> {
        return emptyList()
    }

    internal fun hentInvolveringer(omsorgspengerSaksnummer: OmsorgspengerSaksnummer) : Map<Involvering, List<BehandlingId>> {
        return emptyMap()
    }
}