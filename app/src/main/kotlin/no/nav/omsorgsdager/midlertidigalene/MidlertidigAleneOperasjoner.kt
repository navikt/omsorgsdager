package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Part

internal object MidlertidigAleneOperasjoner : BehandlingOperasjoner<MidlertidigAleneBehandling> {
    override fun map(dbBehandling: DbBehandling, parter: List<Part>): MidlertidigAleneBehandling {
        TODO("Not yet implemented")
    }
}