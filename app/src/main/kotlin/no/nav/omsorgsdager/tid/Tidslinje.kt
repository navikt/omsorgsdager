package no.nav.omsorgsdager.tid

import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.erFørEllerLik
import no.nav.omsorgsdager.tid.Periode.Companion.nesteDag
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class Tidslinje(
    private val initiellePerioder : List<Periode>) {
    private val dagerIkkeIInitiellePerioder = mutableSetOf<LocalDate>()

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

        var current = periode.fom
        while (current.erFørEllerLik(periode.tom)) {
            if (!current.finnesIInitiellePerioder()) {
                dagerIkkeIInitiellePerioder.add(current)
            }
            current = current.nesteDag()
        }

        return this
    }

    private fun LocalDate.finnesIInitiellePerioder() =
        initiellePerioder.any { it.inneholder(this) }

    private fun Set<LocalDate>.periodiser() : MutableList<Periode> {
        val sortert = sorted()
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

    internal fun nyePerioder() : List<Periode> {
        val nyePerioder = dagerIkkeIInitiellePerioder.periodiser()
        nyePerioder.firstOrNull { it.tom == EtterTidslinjeMaks }?.also { periode ->
            nyePerioder.remove(periode)
            periode.copy(tom = TidenesEnde).also { korrigertPeriode ->
                nyePerioder.add(korrigertPeriode)
                logger.info("Håndterer tidenes ende. Bytter ut $this med $korrigertPeriode")
            }
        }
        return nyePerioder
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Tidslinje::class.java)
        private val TidenesEnde = "9999-12-31".dato()
    }
}