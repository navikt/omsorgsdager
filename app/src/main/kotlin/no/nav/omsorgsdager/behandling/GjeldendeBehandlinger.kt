@file:Suppress("UNCHECKED_CAST")

package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnBehandling
import no.nav.omsorgsdager.midlertidigalene.MidlertidigAleneBehandling
import no.nav.omsorgsdager.tid.Gjeldende.gjeldende

internal class GjeldendeBehandlinger(
    alleKroniskSyktBarn: List<KroniskSyktBarnBehandling>,
    alleMidlertidigAlene: List<MidlertidigAleneBehandling>) {
    internal val kroniskSyktBarn = alleKroniskSyktBarn.gjeldende()
    internal val midlertidigAlene = alleMidlertidigAlene.gjeldende()
}