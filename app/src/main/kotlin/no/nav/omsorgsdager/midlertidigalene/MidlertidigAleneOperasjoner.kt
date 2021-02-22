package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.NyBehandling
import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Part

internal object MidlertidigAleneOperasjoner : BehandlingOperasjoner<MidlertidigAleneBehandling> {
    override fun mapTilEksisterendeBehandling(
        dbBehandling: DbBehandling,
        parter: List<Part>): MidlertidigAleneBehandling {
        TODO("Not yet implemented")
    }

    override fun mapTilNyBehandling(
        grunnlag: Json,
        saksnummer: Map<Identitetsnummer, OmsorgspengerSaksnummer>,
        behandlingStatus: BehandlingStatus): Pair<NyBehandling, List<Part>> {
        TODO("Not yet implemented")
    }


}