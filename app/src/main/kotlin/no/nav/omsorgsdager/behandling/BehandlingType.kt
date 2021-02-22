package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnOperasjoner
import no.nav.omsorgsdager.midlertidigalene.MidlertidigAleneOperasjoner

internal enum class BehandlingType(
    internal val operasjoner: BehandlingOperasjoner<out EksisterendeBehandling>) {
    KRONISK_SYKT_BARN(operasjoner = KroniskSyktBarnOperasjoner),
    MIDLERTIDIG_ALENE(operasjoner = MidlertidigAleneOperasjoner)
}