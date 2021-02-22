package no.nav.omsorgsdager.tid

import java.time.*

internal data class Periode(
    internal val fom : LocalDate,
    internal val tom : LocalDate) {

    constructor(iso: String) : this(LocalDate.parse(iso.split("/")[0]), LocalDate.parse(iso.split("/")[1]))
    constructor(år: Int) : this("$år-01-01/$år-12-31")
    constructor(enkeltdag: LocalDate) : this(fom = enkeltdag, tom = enkeltdag)

    init {
        require(tom.isAfter(fom) || fom.isEqual(tom)) {"Ugylidg periode. fom=$fom, tom=$tom"}
    }
    internal fun erFør(periode: Periode) = tom.isBefore(periode.fom)
    internal fun erEtter(periode: Periode) = fom.isAfter(periode.tom)
    internal fun inneholder(dato: LocalDate) = dato in fom..tom

    internal fun overlapperMedMinstEnDag(periode: Periode) = !erFør(periode) && !erEtter(periode)
    internal fun inneholder(periode: Periode) = inneholder(periode.fom) && inneholder(periode.tom)

    override fun toString() = "$fom/$tom"

    internal companion object {
        internal fun utcNå() = ZonedDateTime.now(ZoneOffset.UTC)
        internal fun String.dato() = LocalDate.parse(this)
        internal fun String.periode() = Periode(this)
        private val Oslo = ZoneId.of("Europe/Oslo")
        internal fun ZonedDateTime.toLocalDateOslo() = withZoneSameInstant(Oslo).toLocalDate()
        internal fun LocalDate.startenAvDagenOslo() = ZonedDateTime.of(this, LocalTime.MIDNIGHT, Oslo)
        internal fun LocalDate.sisteDagIÅretOm18År() =
            plusYears(18).withMonth(12).withDayOfMonth(31)
        internal fun LocalDate.erFørEllerLik(annen: LocalDate) = isBefore(annen) || isEqual(annen)
        internal fun LocalDate.erEtterEllerLik(annen: LocalDate) = isAfter(annen) || isEqual(annen)
        internal fun LocalDate.nesteDag() = plusDays(1)
        internal fun LocalDate.forrigeDag() = minusDays(1)
        internal fun Pair<LocalDate?, LocalDate?>.periodeOrNull() = kotlin.runCatching {
            Periode(fom = first!!, tom = second!!)
        }.fold(onSuccess = {it}, onFailure = {null})
    }
}