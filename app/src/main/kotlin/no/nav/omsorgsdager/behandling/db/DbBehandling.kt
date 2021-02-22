package no.nav.omsorgsdager.behandling.db

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.behandling.BehandlingType

internal data class DbBehandling (
    internal val id: BehandlingId,
    internal val type: BehandlingType
)