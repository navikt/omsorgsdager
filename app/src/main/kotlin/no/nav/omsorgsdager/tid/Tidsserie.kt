package no.nav.omsorgsdager.tid

import no.nav.omsorgsdager.tid.Periode.Companion.erFørEllerLik
import no.nav.omsorgsdager.tid.Periode.Companion.nesteDag
import java.time.LocalDate

internal class Tidsserie(
    private val initiellePerioder : List<Periode>) {
    private val nyePerioder = mutableListOf<Periode>()
    internal fun nyePerioder() : List<Periode> = nyePerioder

    internal fun leggTil(periode: Periode) : Tidsserie {
        val dagerIkkeITidsserie = mutableListOf<LocalDate>()
        var current = periode.fom
        while (current.erFørEllerLik(periode.tom)) {
            if (!current.finnesITidsserie()) {
                dagerIkkeITidsserie.add(current)
            }
            current = current.nesteDag()
        }
        nyePerioder.addAll(dagerIkkeITidsserie.periodiser())
        return this
    }

    private fun LocalDate.finnesITidsserie() =
        initiellePerioder.any { it.inneholder(this) } || nyePerioder.any { it.inneholder(this) }

    private fun List<LocalDate>.periodiser() : List<Periode> {
        val sortert = toSet().sorted()
        val perioder = mutableListOf<Periode>()
        for (fom in sortert) {
            if (perioder.any { it.inneholder(fom) }) continue
            perioder.add(
                sortert.filter { it.isAfter(fom) }.lengstMuligPeriodeFom(fom)
            )
        }
        return perioder
    }

    private fun List<LocalDate>.lengstMuligPeriodeFom(fom: LocalDate): Periode {
        var tom = fom
        while (tom.nesteDag() in this) {
            tom = tom.nesteDag()
        }
        return Periode(fom = fom, tom = tom)
    }
}