package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Tidslinje
import java.time.LocalDate
import java.time.ZonedDateTime

internal interface Vedtak {
    val søkersIdentitetsnummer: Identitetsnummer
    val saksnummer: Saksnummer
    val behandlingId: BehandlingId
    val status: VedtakStatus
    val statusSistEndret: ZonedDateTime
    val barn: Any
    val periode: Periode
    fun kopiMedNyPeriode(nyPeriode: Periode) : Vedtak

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val ignorerStatuser = setOf(VedtakStatus.FORESLÅTT, VedtakStatus.FORKASTET)
        internal fun <V: Vedtak> List<V>.gjeldendeVedtak() : List<V> = asSequence()
            .filterNot { it.status in ignorerStatuser }
            .sortedByDescending { it.statusSistEndret }
            .groupBy { it.barn }
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
                fom == null && tom == null -> true
                else -> Periode(fom = fom?:tom!!, tom = tom?:fom!!).overlapperMedMinstEnDag(periode)
            }

        private fun <V: Vedtak>List<V>.gjeldendeVedtakPerBarn() : List<V> {
            if (isEmpty()) return emptyList()
            require(all { it.barn == first().barn }) { "Kan kun gjøres for samme barn" }

            val gjeldendeVedtak = mutableListOf<V>()

            forEach { vedtak ->
                Tidslinje(gjeldendeVedtak.map { it.periode })
                    .leggTil(vedtak.periode)
                    .nyePerioder()
                    .forEach { nyPeriode ->
                        gjeldendeVedtak.add(vedtak.kopiMedNyPeriode(nyPeriode = nyPeriode) as V)
                    }
            }

            return gjeldendeVedtak
        }
    }
}