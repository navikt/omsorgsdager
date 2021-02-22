package no.nav.omsorgsdager.behandling.db

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.behandling.NyBehandling
import javax.sql.DataSource

internal class BehandlingRepository(
    private val dataSource: DataSource) {
    internal fun lagre(behandling: NyBehandling) : BehandlingId {
        return 0L
    }

    internal fun hentEn(behandlingId: K9BehandlingId) : DbBehandling? {
        return null
    }

    internal fun hentAlle(saksnummer: K9Saksnummer) : List<DbBehandling> {
        return emptyList()
    }

    internal fun hentAlle(behandlingIder: List<BehandlingId>) : List<DbBehandling> {
        return emptyList()
    }
}