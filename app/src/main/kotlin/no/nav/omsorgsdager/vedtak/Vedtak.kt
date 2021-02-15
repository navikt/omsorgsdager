package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Tidslinje
import java.time.LocalDate
import java.time.ZonedDateTime

internal interface Vedtak {
    val saksnummer: Saksnummer
    val behandlingId: BehandlingId
    val status: VedtakStatus
    val statusSistEndret: ZonedDateTime
    val periode: Periode?
    val etGjeldendeVedtakPer: Any
    val involverteIdentitetsnummer: Set<Identitetsnummer>
    val grunnlag: Json
    fun kopiMedNyPeriode(nyPeriode: Periode) : Vedtak

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val ignorerStatuser = setOf(VedtakStatus.FORESLÅTT, VedtakStatus.FORKASTET)
        internal fun <V: Vedtak> List<V>.gjeldendeVedtak() : List<V> = asSequence()
            .filterNot { it.status in ignorerStatuser }
            .also { require(all { it.periode != null }) {
                "Alle vedtak som har annen status enn $ignorerStatuser må ha en periode satt."
            }}
            .sortedByDescending { it.statusSistEndret }
            .groupBy { it.etGjeldendeVedtakPer }
            .map { it.value.gjeldendeVedtakPerBarn() }
            .flatten()
            .toList()

        internal fun <V: Vedtak> List<V>.filtrerPåDatoer(
            fom: LocalDate?,
            tom: LocalDate?
        ) = filter { it.erInnenforDatoer(fom,tom) }

        internal fun Vedtak.erInnenforDatoer(
            fom: LocalDate?,
            tom: LocalDate?) = when {
                periode == null -> true
                fom == null && tom == null -> true
                else -> Periode(fom = fom?:tom!!, tom = tom?:fom!!).overlapperMedMinstEnDag(periode!!)
            }

        private fun <V: Vedtak>List<V>.gjeldendeVedtakPerBarn() : List<V> {
            if (isEmpty()) return emptyList()
            require(all { it.etGjeldendeVedtakPer == first().etGjeldendeVedtakPer }) { "Alle må være for ${first().etGjeldendeVedtakPer}" }

            val gjeldendeVedtak = mutableListOf<V>()

            forEach { vedtak ->
                Tidslinje(gjeldendeVedtak.map { it.periode!! })
                    .leggTil(vedtak.periode!!)
                    .nyePerioder()
                    .forEach { nyPeriode ->
                        gjeldendeVedtak.add(vedtak.kopiMedNyPeriode(nyPeriode = nyPeriode) as V)
                    }
            }

            return gjeldendeVedtak
        }
    }
}