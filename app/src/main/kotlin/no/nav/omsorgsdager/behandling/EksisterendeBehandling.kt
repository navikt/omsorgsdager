package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.tid.Gjeldende
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal interface EksisterendeBehandling : Gjeldende.KanUtledeGjeldende {
    val k9Saksnummer: K9Saksnummer
    val k9behandlingId: K9BehandlingId
    val status: BehandlingStatus
    override val tidspunkt: ZonedDateTime
    override val periode: Periode
    val involverteIdentitetsnummer: Set<Identitetsnummer>
}