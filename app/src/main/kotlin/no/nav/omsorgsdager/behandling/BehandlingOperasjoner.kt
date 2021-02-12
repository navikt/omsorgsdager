package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.vedtak.Vedtak
import java.time.ZonedDateTime

internal interface BehandlingOperasjoner<V: Vedtak> {
    suspend fun hent(behandlingId: BehandlingId): Behandling<V>?
    suspend fun hentAlle(saksnummer: Saksnummer): List<Behandling<V>>
    fun behandlingDto(behandling: Behandling<V>) : Any

    suspend fun preOpprett(grunnlag: Json) : Set<Identitetsnummer>
    suspend fun opprett(grunnlag: Json, correlationId: CorrelationId): Behandling<V>

    suspend fun løsninger(behandlingId: BehandlingId, grunnlag: Json): Behandling<V>

    suspend fun innvilg(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<V>
    suspend fun avslå(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<V>
    suspend fun forkast(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<V>
}