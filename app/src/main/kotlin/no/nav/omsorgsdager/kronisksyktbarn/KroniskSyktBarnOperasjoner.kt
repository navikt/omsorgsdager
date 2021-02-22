package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Part

internal object KroniskSyktBarnOperasjoner : BehandlingOperasjoner<KroniskSyktBarnBehandling> {
    override fun map(dbBehandling: DbBehandling, parter: List<Part>): KroniskSyktBarnBehandling {
        TODO("Not yet implemented")
    }

}