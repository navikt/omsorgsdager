package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import java.time.ZonedDateTime

internal class MidlertidigAleneOperasjoner : BehandlingOperasjoner<MidlertidigAleneVedtak> {
    override suspend fun hent(behandlingId: BehandlingId): Behandling<MidlertidigAleneVedtak>? {
        TODO("Not yet implemented")
    }

    override suspend fun hentAlle(saksnummer: Saksnummer): List<Behandling<MidlertidigAleneVedtak>> {
        TODO("Not yet implemented")
    }

    override fun behandlingDto(behandling: Behandling<MidlertidigAleneVedtak>): Any {
        TODO("Not yet implemented")
    }

    override suspend fun preOpprett(grunnlag: Json): Set<Identitetsnummer> {
        TODO("Not yet implemented")
    }

    override suspend fun opprett(grunnlag: Json, correlationId: CorrelationId): Behandling<MidlertidigAleneVedtak> {
        TODO("Not yet implemented")
    }

    override suspend fun løsninger(behandlingId: BehandlingId, grunnlag: Json): Behandling<MidlertidigAleneVedtak> {
        TODO("Not yet implemented")
    }

    override suspend fun innvilg(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<MidlertidigAleneVedtak> {
        TODO("Not yet implemented")
    }

    override suspend fun avslå(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<MidlertidigAleneVedtak> {
        TODO("Not yet implemented")
    }

    override suspend fun forkast(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<MidlertidigAleneVedtak> {
        TODO("Not yet implemented")
    }
}