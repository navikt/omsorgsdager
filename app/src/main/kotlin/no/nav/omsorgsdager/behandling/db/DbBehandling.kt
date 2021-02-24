package no.nav.omsorgsdager.behandling.db

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal data class DbBehandling (
    internal val id: BehandlingId,
    internal val type: BehandlingType,
    internal val k9Saksnummer: K9Saksnummer,
    internal val k9behandlingId: K9BehandlingId,
    internal val status: BehandlingStatus,
    internal val tidspunkt: ZonedDateTime,
    internal val periode: Periode
)
