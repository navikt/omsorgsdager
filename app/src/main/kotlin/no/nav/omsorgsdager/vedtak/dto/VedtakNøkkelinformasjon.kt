package no.nav.omsorgsdager.vedtak.dto

import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.aksjonspunkt.kanFastsettes
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus

internal object VedtakNøkkelinformasjon {
    internal data class Response private constructor(
        val status: VedtakStatus,
        val potensielleStatuser: Set<VedtakStatus>,
        val uløsteAksjonspunkter: Map<String, Any>) {
        internal constructor(
            vedtak: Vedtak,
            aksjonspunkter: Aksjonspunkter) : this(
            status = vedtak.status,
            potensielleStatuser = when (vedtak.status) {
                VedtakStatus.FORSLAG -> when {
                    aksjonspunkter.løsteAksjonspunkter.kanFastsettes() -> setOf(
                        VedtakStatus.FASTSATT, VedtakStatus.DEAKTIVERT
                    )
                    else -> setOf(VedtakStatus.DEAKTIVERT)
                }
                else -> emptySet()
            },
            uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter.associateBy { it.navn }.mapValues { Any() }
        )
    }
}