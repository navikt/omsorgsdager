package no.nav.omsorgsdager.kronisksyktbarn.dto

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnVedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.LocalDate

internal data class HentKroniskSkytBarnListeResponse(
    val vedtak: List<HentKroniskSyktBarnResponse>
)

internal data class HentKroniskSyktBarnResponse(
    val barn: Barn,
    val behandlingId: BehandlingId,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val status: VedtakStatus,
    val uløsteAksjonspunkter: Map<String, Any>,
    val løsteAksjonspunkter: Map<String, Any>) {
    constructor(vedtak: KroniskSyktBarnVedtak, aksjonspunkter: Aksjonspunkter) : this(
        barn = vedtak.barn,
        behandlingId = vedtak.behandlingId,
        gyldigFraOgMed = vedtak.periode.fom,
        gyldigTilOgMed = vedtak.periode.tom,
        status = vedtak.status,
        uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter
            .associateBy { it.navn }
            .mapValues { Any() },
        løsteAksjonspunkter = aksjonspunkter.løsteAksjonspunkter
            .associateBy { it.navn }
            .mapValues { it.value.løsning.map }
    )
}