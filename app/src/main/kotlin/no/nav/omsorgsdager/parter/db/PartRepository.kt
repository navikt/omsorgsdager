package no.nav.omsorgsdager.parter.db

import kotliquery.TransactionalSession
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.parter.Part
import javax.sql.DataSource

internal class PartRepository(
    private val dataSource: DataSource) {
    internal fun hentParter(behandlingIder: List<BehandlingId>) : List<DbPart> {
        return emptyList()
    }

    internal fun hentInvolveringer(omsorgspengerSaksnummer: OmsorgspengerSaksnummer) : Map<Involvering, List<BehandlingId>> {
        return emptyMap()
    }

    internal fun hentOmsorgspengerSaksnummer(identitetsnummer: Identitetsnummer) : OmsorgspengerSaksnummer? {
        return null
    }

    internal companion object {
        internal fun TransactionalSession.leggTilParter(
            behandlingId: BehandlingId,
            parter: List<Part>) {

        }
    }
}