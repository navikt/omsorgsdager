package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.kronisksyktbarn.dto.Barn
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal data class KroniskSyktBarnVedtak(
    override val saksnummer: Saksnummer,
    override val behandlingId: BehandlingId,
    override val status: VedtakStatus,
    override val statusSistEndret: ZonedDateTime,
    override val barn: Barn,
    override val periode: Periode) : Vedtak {
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )
}