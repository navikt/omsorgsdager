package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.BehovssekvensId
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal data class NyBehandling(
    internal val behovssekvensId: BehovssekvensId,
    internal val saksnummer: K9Saksnummer,
    internal val behandlingId: K9BehandlingId,
    internal val tidspunkt: ZonedDateTime,
    internal val type: BehandlingType,
    internal val status: BehandlingStatus,
    internal val periode: Periode,
    internal val grunnlag: Json)
