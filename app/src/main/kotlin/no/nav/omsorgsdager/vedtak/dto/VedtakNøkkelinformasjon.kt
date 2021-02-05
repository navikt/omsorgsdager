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
        internal constructor(vedtakOgBehov: Pair<Vedtak, Behov>) : this(
            status = vedtakOgBehov.first.status,
            potensielleStatuser = vedtakOgBehov.potensielleStatuser()
                .associateBy { it.name }.mapValues { Any() },
            uløsteBehov = vedtakOgBehov.second.uløsteBehov
                .associateBy { it.navn }.mapValues { Any() }
        )
    }

    private fun Pair<Vedtak, Behov>.potensielleStatuser() = when (first.status) {
        VedtakStatus.FORESLÅTT -> when {
            second.løsteBehov.kanInnvilges() -> setOf(
                VedtakStatus.INNVILGET, VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET
            )
            else -> setOf(VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET)
        }
        else -> emptySet()
    }

    internal fun Pair<Vedtak, Behov>.kanInnvilges() = potensielleStatuser().contains(VedtakStatus.INNVILGET) && second.uløsteBehov.isEmpty()

    internal fun Pair<Vedtak, Behov>.kanAvslås() = potensielleStatuser().contains(VedtakStatus.AVSLÅTT)

    internal fun Pair<Vedtak, Behov>.kanForkastes() = potensielleStatuser().contains(VedtakStatus.FORKASTET)
}