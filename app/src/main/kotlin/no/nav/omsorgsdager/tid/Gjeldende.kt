@file:Suppress("UNCHECKED_CAST")

package no.nav.omsorgsdager.tid

import java.time.ZonedDateTime

internal object Gjeldende {
    interface KanUtledeGjeldende {
        val tidspunkt: ZonedDateTime
        val enPer: Any
        val periode: Periode
        fun kopiMedNyPeriode(nyPeriode: Periode) : KanUtledeGjeldende
    }

    internal fun <KUG: KanUtledeGjeldende> List<KUG>.gjeldende() : List<KUG> {
        if (isEmpty()) return emptyList()
        require(all { it.javaClass == first().javaClass }) {
            "Alt som skal sammenstilles som gjeldede må være av samme type"
        }

        return asSequence()
            .sortedByDescending { it.tidspunkt }
            .groupBy { it.enPer }
            .map { it.value.gjeldendePer() }
            .flatten()
            .toList()
    }

    private fun<KUG: KanUtledeGjeldende> List<KUG>.gjeldendePer() : List<KUG> {
        if (isEmpty()) return emptyList()
        require(all { it.enPer == first().enPer }) {
            "Alle må gjeldende ${first().enPer}"
        }

        val gjeldende = mutableListOf<KUG>()

        forEach { behandling ->
            Tidslinje(gjeldende.map { it.periode })
                .leggTil(behandling.periode)
                .nyePerioder()
                .forEach { nyPeriode ->
                    gjeldende.add(behandling.kopiMedNyPeriode(nyPeriode = nyPeriode) as KUG)
                }
        }

        return gjeldende
    }
}