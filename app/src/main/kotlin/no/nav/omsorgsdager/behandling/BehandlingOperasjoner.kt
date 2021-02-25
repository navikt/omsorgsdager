package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.BehovssekvensId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Part

internal interface BehandlingOperasjoner<EB: EksisterendeBehandling> {
    fun mapTilEksisterendeBehandling(dbBehandling: DbBehandling, parter: List<Part>) : EB
    fun mapTilNyBehandling(behovssekvensId: BehovssekvensId, grunnlag: Json, saksnummer: Map<Identitetsnummer, OmsorgspengerSaksnummer>, behandlingStatus: BehandlingStatus) : Pair<NyBehandling, List<Part>>
}