package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.EksisterendeBehandling
import no.nav.omsorgsdager.parter.Motpart
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal data class MidlertidigAleneBehandling (
    override val k9Saksnummer: K9Saksnummer,
    override val k9behandlingId: K9BehandlingId,
    override val tidspunkt: ZonedDateTime,
    override val periode: Periode,
    internal val annenForelder: Motpart,
    internal val søker: Søker,
    override val status: BehandlingStatus) : EksisterendeBehandling {
    override val enPer: K9Saksnummer = k9Saksnummer
    override val involverteIdentitetsnummer = setOf(søker.identitetsnummer, annenForelder.identitetsnummer)
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(periode = nyPeriode)
}
