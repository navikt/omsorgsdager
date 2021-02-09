package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal data class MidlertidigAleneVedtak(
    override val søkersIdentitetsnummer: Identitetsnummer,
    override val saksnummer: Saksnummer,
    override val behandlingId: BehandlingId,
    override val status: VedtakStatus,
    override val statusSistEndret: ZonedDateTime,
    override val barn: Any,
    override val periode: Periode) : Vedtak {
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )
}