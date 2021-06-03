package no.nav.omsorgsdager.tid

import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.erFørEllerLik
import no.nav.omsorgsdager.tid.Periode.Companion.nesteDag
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class Tidslinje(
    private val initiellePerioder : List<Periode>) {
    private val nyePerioder = mutableListOf<Periode>()
    internal fun nyePerioder() : List<Periode> = nyePerioder

    private val TidslinjeMaks = (initiellePerioder.minByOrNull { it.fom }?.fom?: LocalDate.now()).sisteDagIÅretOm18År()
    private val EtterTidslinjeMaks = TidslinjeMaks.plusDays(1)
    private fun Periode.sanitize() = when {
        tom.erFørEllerLik(TidslinjeMaks) -> this
        tom == TidenesEnde -> this.copy(tom = EtterTidslinjeMaks)
        else -> throw IllegalStateException("Ugyldig tilOgMed-dato $tom")
    }

    init {
        logger.trace("InitiellePerioder=$initiellePerioder")
    }

    internal fun leggTil(periodeInn: Periode) : Tidslinje {
        val periode = periodeInn.sanitize()
        val (leggTilPerioder, beskrivelse) = when {
            periode.finnesITidslinje() -> emptyList<Periode>().let { it to "Hele perioden finnes allerede i tidslinjen." }
            !periode.overlapperMedMinstEnDagITidslinje() -> listOf(periode).let { it to "Overlapper ikke med noen dager i tidslinjen, legges til i sin helhet." }
            else -> {
                val dagerIkkeITidslinje = mutableListOf<LocalDate>()
                var current = periode.fom
                while (current.erFørEllerLik(periode.tom)) {
                    if (!current.finnesITidlinje()) {
                        dagerIkkeITidslinje.add(current)
                    }
                    current = current.nesteDag()
                }
                dagerIkkeITidslinje.periodiser() to "Kalkulert nye perioder som skal legges til."
            }
        }

        nyePerioder.addAll(leggTilPerioder)
        logger.trace("Periode=[$periode], LeggesTil=${leggTilPerioder}, NyePerioder=${nyePerioder} Beskrivelse=[$beskrivelse]")

        håndterTidenesEnde()
        return this
    }

    private fun Periode.overlapperMedMinstEnDagITidslinje() =
        initiellePerioder.any { it.overlapperMedMinstEnDag(this) } || nyePerioder.any { it.overlapperMedMinstEnDag(this) }

    private fun Periode.finnesITidslinje() =
        initiellePerioder.any { it.inneholder(this) } || nyePerioder.any { it.inneholder(this) }

    private fun LocalDate.finnesITidlinje() =
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

    private fun håndterTidenesEnde() {
        nyePerioder.firstOrNull { it.tom == EtterTidslinjeMaks }?.apply {
            nyePerioder.remove(this)
            nyePerioder.add(this.copy(tom = TidenesEnde))
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Tidslinje::class.java)
        private val TidenesEnde = "9999-12-31".dato()
    }
}