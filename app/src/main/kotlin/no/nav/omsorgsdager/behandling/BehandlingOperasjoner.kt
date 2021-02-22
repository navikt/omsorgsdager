package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Part

internal interface BehandlingOperasjoner<EB: EksisterendeBehandling> {
    fun map(dbBehandling: DbBehandling, parter: List<Part>) : EB
}