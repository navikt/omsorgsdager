package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.kronisksyktbarn.dto.Barn
import no.nav.omsorgsdager.kronisksyktbarn.dto.Søker
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal data class KroniskSyktBarnVedtak(
    override val saksnummer: Saksnummer,
    override val behandlingId: BehandlingId,
    override val status: VedtakStatus,
    override val statusSistEndret: ZonedDateTime,
    override val periode: Periode,
    internal val barn: Barn,
    internal val søker: Søker) : Vedtak {
    override val etGjeldendeVedtakPer: Barn = barn

    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )

    override val involverteIdentitetsnummer : Set<Identitetsnummer> = setOf(
        søker.identitetsnummer,
        barn.identitetsnummer
    ).filterNotNull().toSet()
}