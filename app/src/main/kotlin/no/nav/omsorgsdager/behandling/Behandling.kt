package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.kanInnvilges
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus

internal data class Behandling<V: Vedtak> (
    internal val vedtak: V,
    internal val behov: Behov) {
    internal val potensielleStatuser = when(vedtak.status) {
        VedtakStatus.FORESLÅTT -> when {
            behov.løsteBehov.kanInnvilges() -> setOf(
                VedtakStatus.INNVILGET, VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET
            )
            else -> setOf(VedtakStatus.AVSLÅTT, VedtakStatus.FORKASTET)
        }
        else -> emptySet()
    }
    internal val kanInnvilges: Boolean = potensielleStatuser.contains(VedtakStatus.INNVILGET) && behov.uløsteBehov.isEmpty()
    internal val kanAvslås: Boolean = potensielleStatuser.contains(VedtakStatus.AVSLÅTT)
    internal val kanForkastes: Boolean = potensielleStatuser.contains(VedtakStatus.FORKASTET)
}
