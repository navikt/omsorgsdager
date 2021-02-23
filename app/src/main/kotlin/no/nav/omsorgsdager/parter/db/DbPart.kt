package no.nav.omsorgsdager.parter.db

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.parter.Part

internal data class DbPart(
    internal val behandlingId: BehandlingId,
    internal val part: Part
)