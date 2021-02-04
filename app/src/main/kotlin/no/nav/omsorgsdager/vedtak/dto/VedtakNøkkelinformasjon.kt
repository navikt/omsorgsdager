package no.nav.omsorgsdager.vedtak.dto

import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.aksjonspunkt.kanInnvilges
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus

internal object VedtakNøkkelinformasjon {
    internal data class Response private constructor(
        val status: VedtakStatus,
        val potensielleStatuser: Map<String, Any>,
        val uløsteAksjonspunkter: Map<String, Any>) {
        internal constructor(
            vedtak: Vedtak,
            aksjonspunkter: Aksjonspunkter) : this(
            status = vedtak.status,
            potensielleStatuser = when (vedtak.status) {
                VedtakStatus.FORESLÅTT -> when {
                    aksjonspunkter.løsteAksjonspunkter.kanInnvilges() -> setOf(
                        VedtakStatus.INNVILGET, VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET
                    )
                    else -> setOf(VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET)
                }
                else -> emptySet()
            }.associateBy { it.name }.mapValues { Any() },
            uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter.associateBy { it.navn }.mapValues { Any() }
        )
    }
}