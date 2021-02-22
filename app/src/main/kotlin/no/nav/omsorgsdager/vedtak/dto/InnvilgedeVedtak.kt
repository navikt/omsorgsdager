package no.nav.omsorgsdager.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.GjeldendeBehandlinger
import no.nav.omsorgsdager.tid.Gjeldende
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import no.nav.omsorgsdager.vedtak.dto.Kilde.Companion.somKilder
import java.time.LocalDate
import java.time.ZonedDateTime

internal data class InnvilgedeVedtak(
    @get:JsonProperty("kronisk-sykt-barn") val kroniskSyktBarn: List<KroniskSyktBarnInnvilgetVedtak>,
    @get:JsonProperty("midlertidig-alene") val midlertidigAlene: List<MidlertidigAleneInnvilgetVedtak>) {
    @get:JsonIgnore val isEmpty = kroniskSyktBarn.isEmpty() && midlertidigAlene.isEmpty()
    internal companion object {
        internal fun gjeldendeBehandlingerSomInnvilgedeVedtak(gjeldendeBehandlinger: GjeldendeBehandlinger?)  = when (gjeldendeBehandlinger) {
            null -> InnvilgedeVedtak(
                kroniskSyktBarn = emptyList(),
                midlertidigAlene = emptyList()
            )
            else -> InnvilgedeVedtak(
                kroniskSyktBarn = gjeldendeBehandlinger.kroniskSyktBarn.filter { it.status == BehandlingStatus.INNVILGET }.map { KroniskSyktBarnInnvilgetVedtak(
                    tidspunkt = it.tidspunkt,
                    barn = Barn(identitetsnummer = it.barn.identitetsnummer?.toString(), fødselsdato = it.barn.fødselsdato),
                    periode = it.periode,
                    kilder = it.k9behandlingId.somKilder()
                )},
                midlertidigAlene = gjeldendeBehandlinger.kroniskSyktBarn.filter { it.status == BehandlingStatus.INNVILGET }.map { MidlertidigAleneInnvilgetVedtak(
                    tidspunkt = it.tidspunkt,
                    periode = it.periode,
                    kilder = it.k9behandlingId.somKilder()
                )}
            )
        }
        internal fun ingenInnvilgedeVedtak() = InnvilgedeVedtak(kroniskSyktBarn = emptyList(), midlertidigAlene = emptyList())
    }
}

data class Kilde(
    val id: String,
    val type: String) {
    internal companion object {
        internal fun K9BehandlingId.somKilder() = setOf(Kilde(id = "$this", type = "k9-sak"))
    }
}

data class Barn(
    val identitetsnummer: String?,
    val fødselsdato: LocalDate
)

internal interface InnvilgetVedtak : Gjeldende.KanUtledeGjeldende {
    val kilder: Set<Kilde>
    fun vedtatt() = tidspunkt.toLocalDateOslo()
    fun gyligFraOgMed() = periode.fom
    fun gyldigTilOgMed() = periode.tom
}

internal data class KroniskSyktBarnInnvilgetVedtak(
    val barn: Barn,
    override val kilder: Set<Kilde>,
    @get:JsonIgnore override val tidspunkt: ZonedDateTime,
    @get:JsonIgnore override val periode: Periode) : InnvilgetVedtak {
    @get:JsonIgnore override val enPer = barn
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = periode
    )
}

internal data class MidlertidigAleneInnvilgetVedtak(
    override val kilder: Set<Kilde>,
    @get:JsonIgnore override val tidspunkt: ZonedDateTime,
    @get:JsonIgnore override val periode: Periode) : InnvilgetVedtak {
    @get:JsonIgnore override val enPer = MidlertidigAleneInnvilgetVedtak::class
    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = periode
    )
}