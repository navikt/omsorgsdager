package no.nav.omsorgsdager.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.tid.Gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo

import java.time.LocalDate
import java.time.ZonedDateTime

internal data class InnvilgedeVedtak(
    val kroniskSyktBarn: List<KroniskSyktBarnInnvilgetVedtak> = emptyList(),
    val midlertidigAlene: List<MidlertidigAleneInnvilgetVedtak> = emptyList()
)

data class Kilde(
    val id: String,
    val type: String) {
    internal companion object {
        internal fun K9BehandlingId.somKilde() = Kilde(id = "$this", type = "K9-sak")
        internal fun K9BehandlingId.somKilder() = setOf(this.somKilde())
    }
}

data class Barn(
    val identitetsnummer: String?,
    val fødselsdato: LocalDate
)

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
    @get:JsonIgnore override val periode: Periode) : InnvilgetVedtak {
    @get:JsonIgnore override val enPer = barn
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