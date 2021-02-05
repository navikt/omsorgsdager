package no.nav.omsorgsdager.vedtak.dto

import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.kanInnvilges
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus

internal object VedtakNøkkelinformasjon {
    internal data class Response private constructor(
        val status: VedtakStatus,
        val potensielleStatuser: Map<String, Any>,
        val uløsteBehov: Map<String, Any>) {
        internal constructor(
            vedtak: Vedtak,
            behov: Behov) : this(
            status = vedtak.status,
            potensielleStatuser = when (vedtak.status) {
                VedtakStatus.FORESLÅTT -> when {
                    behov.løsteBehov.kanInnvilges() -> setOf(
                        VedtakStatus.INNVILGET, VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET
                    )
                    else -> setOf(VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET)
                }
                else -> emptySet()
            }.associateBy { it.name }.mapValues { Any() },
            uløsteBehov = behov.uløsteBehov.associateBy { it.navn }.mapValues { Any() }
        )
    }
}