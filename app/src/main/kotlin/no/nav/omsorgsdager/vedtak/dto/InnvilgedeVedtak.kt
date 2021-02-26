package no.nav.omsorgsdager.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import java.time.LocalDate

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

internal data class InnvilgedeVedtak(
    val kroniskSyktBarn: List<KroniskSyktBarnInnvilgetVedtak> = emptyList(),
    val midlertidigAlene: List<MidlertidigAleneInnvilgetVedtak> = emptyList()
)

data class Kilde(
    val id: String,
    val type: String) {
    internal companion object {
        internal fun K9BehandlingId.somKilde() = Kilde(id = "$this", type = "K9-Sak")
        internal fun K9BehandlingId.somKilder() = setOf(this.somKilde())
    }
}

data class Barn private constructor(
    val identitetsnummer: String?,
    val fødselsdato: LocalDate,
    val omsorgspengerSaksnummer: String?) {
    internal constructor(
        identitetsnummer: Identitetsnummer,
        fødselsdato: LocalDate,
        omsorgspengerSaksnummer: OmsorgspengerSaksnummer) : this(
        identitetsnummer = "$identitetsnummer",
        fødselsdato = fødselsdato,
        omsorgspengerSaksnummer = "$omsorgspengerSaksnummer"
    )
    internal constructor(
        identitetsnummer: Identitetsnummer?,
        fødselsdato: LocalDate) : this (
        identitetsnummer = identitetsnummer?.toString(),
        fødselsdato = fødselsdato,
        omsorgspengerSaksnummer = null
    )

    internal companion object {
        private val ddMMyy = DateTimeFormatter.ofPattern("ddMMyy")
        private fun DateTimeFormatter.parseOrNull(tekst: String) = kotlin.runCatching {
            parse(tekst) { temporal: TemporalAccessor? -> LocalDate.from(temporal) }
        }.fold(onSuccess = {it}, onFailure = {null})
        private fun String.erFnr() = ddMMyy.parseOrNull(this.substring(0,6)) != null

        internal fun List<Barn>.sammenlignPå() : (barn: Barn) -> Any = when {
            isEmpty() -> { barn: Barn -> barn.fødselsdato }
            // Om alle barn har et saksnummer sammenligner vi kun på det
            all { it.omsorgspengerSaksnummer != null } -> { barn: Barn -> barn.omsorgspengerSaksnummer!! }
            // Om ingen barn har samme fødselsdato sammenligner vi på det
            map { it.fødselsdato }.toSet().size == size -> { barn: Barn -> barn.fødselsdato }
            // Om alle barn har fødselsnummer (ingen D-nummer eller andre identitetsnummer) kan vi sammenligne på identitetsnummer
            all { it.identitetsnummer != null && it.identitetsnummer.erFnr() } -> { barn: Barn -> barn.identitetsnummer!! }
            // Kun samme identitetsnummer
            all { it.identitetsnummer == first().identitetsnummer } -> { barn: Barn -> barn.identitetsnummer!! }
            // Om det eneste som er satt er fødselsdato eller identitetsnummer er en blanding av forskjellige type identitetsnummer
            // Sammenligner vi kun på fødselsdato
            else -> { barn: Barn -> barn.fødselsdato }
        }
    }
}

internal interface InnvilgetVedtak : Gjeldende.KanUtledeGjeldende {
    val kilder: Set<Kilde>
    @JsonProperty("vedtatt") fun vedtatt() = tidspunkt.toLocalDateOslo()
    @JsonProperty("gyldigFraOgMed") fun gyldigFraOgMed() = periode.fom
    @JsonProperty("gyldigTilOgMed") fun gyldigTilOgMed() = periode.tom
}

internal data class KroniskSyktBarnInnvilgetVedtak(
    val barn: Barn,
    override val kilder: Set<Kilde>,
    @get:JsonIgnore override val tidspunkt: ZonedDateTime,
    @get:JsonIgnore override val periode: Periode,
    @get:JsonIgnore override val enPer: Any = Barn) : InnvilgetVedtak {
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )
}

internal data class MidlertidigAleneInnvilgetVedtak(
    override val kilder: Set<Kilde>,
    @get:JsonIgnore override val tidspunkt: ZonedDateTime,
    @get:JsonIgnore override val periode: Periode) : InnvilgetVedtak {
    @get:JsonIgnore override val enPer = MidlertidigAleneInnvilgetVedtak::class
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )
}