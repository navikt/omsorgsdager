package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Tidslinje
import java.time.ZonedDateTime

typealias Saksnummer = String
typealias BehandlingId = String

internal interface Vedtak {
    val saksnummer: Saksnummer
    val behandlingId: BehandlingId
    val status: VedtakStatus
    val statusSistEndret: ZonedDateTime
    val barn: Any
    val periode: Periode
    fun kopiMedNyPeriode(nyPeriode: Periode) : Vedtak

    companion object {
        internal fun List<Vedtak>.gjeldendeVedtak() = asSequence()
            .filterNot { it.status == VedtakStatus.FORSLAG }
            .sortedByDescending { it.statusSistEndret }
            .groupBy { it.barn }
            .map { it.value.gjeldendeVedtakPerBarn() }
            .flatten()
            .toList()

        private fun List<Vedtak>.gjeldendeVedtakPerBarn() : List<Vedtak> {
            if (isEmpty()) return emptyList()
            require(all { it.barn == first().barn }) { "Kan kun gjøres for samme barn" }

            val gjeldendeVedtak = mutableListOf<Vedtak>()

            forEach { vedtak ->
                Tidslinje(gjeldendeVedtak.map { it.periode })
                    .leggTil(vedtak.periode)
                    .nyePerioder()
                    .forEach { nyPeriode ->
                        gjeldendeVedtak.add(vedtak.kopiMedNyPeriode(nyPeriode = nyPeriode))
                    }
            }

            return gjeldendeVedtak
        }
    }
}