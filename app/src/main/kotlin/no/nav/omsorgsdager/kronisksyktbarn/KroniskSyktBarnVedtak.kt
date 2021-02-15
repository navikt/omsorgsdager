package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behov.AutomatiskLøstBehov
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.somUløstBehov
import no.nav.omsorgsdager.kronisksyktbarn.dto.OpprettKroniskSyktBarn
import no.nav.omsorgsdager.lovverk.Lovanvendelser
import no.nav.omsorgsdager.lovverk.Folketrygdeloven
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.LocalDate
import java.time.ZonedDateTime

internal data class KroniskSyktBarnVedtak(
    override val saksnummer: Saksnummer,
    override val behandlingId: BehandlingId,
    override val status: VedtakStatus,
    override val statusSistEndret: ZonedDateTime,
    override val periode: Periode,
    override val grunnlag: Json,
    internal val søkersIdentitetsnummer: Identitetsnummer,
    internal val barn: Barn) : Vedtak {

    internal data class Barn(internal val fødselsdato: LocalDate, internal val identitetsnummer: Identitetsnummer?)

    override val etGjeldendeVedtakPer: Barn = barn

    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )

    override val involverteIdentitetsnummer : Set<Identitetsnummer> = setOf(
        søkersIdentitetsnummer,
        barn.identitetsnummer
    ).filterNotNull().toSet()

    internal companion object {
        internal fun fraGrunnlag(grunnlag: Json) : Pair<KroniskSyktBarnVedtak, Behov> {
            val deserialized = grunnlag.deserialize<OpprettKroniskSyktBarn.Grunnlag>()
            val lovanvendelseBuilder = Lovanvendelser.Builder()

            val søknadMottatt = deserialized.søknadMottatt.toLocalDateOslo()
            val sisteDagIÅretBarnetFyller18 = deserialized.barn.fødselsdato.sisteDagIÅretOm18År()

            val periode = when (søknadMottatt.isAfter(sisteDagIÅretBarnetFyller18)) {
                true -> {
                    lovanvendelseBuilder.avslått(Folketrygdeloven.UtÅretBarnetFyller18,
                        "Barnet har allerede fylt 18 år.")
                    Periode(enkeltdag = søknadMottatt)
                }
                false -> {
                    lovanvendelseBuilder.innvilget(Folketrygdeloven.UtÅretBarnetFyller18,
                        "Perioden gjelder fra dagen søknaden ble mottatt ut året barnet fyller 18 år.")
                    Periode(fom = søknadMottatt, tom = sisteDagIÅretBarnetFyller18)
                }
            }

            val vurdertPeriode = AutomatiskLøstBehov(
                navn = "VURDERE_PERIODE_FOR_KRONISK_SYKT_BARN",
                versjon = 1,
                lovanvendelser = lovanvendelseBuilder.build(),
                løsning = """
                    { "fom": "${periode.fom}", "tom": "${periode.tom}" }
                """.trimIndent().somJson()
            )

            val uløsteBehov = setOf("VURDERE_KRONISK_SYKT_BARN".somUløstBehov())

            return KroniskSyktBarnVedtak(
                saksnummer = deserialized.saksnummer,
                behandlingId = deserialized.behandlingId,
                status = VedtakStatus.FORESLÅTT,
                statusSistEndret = deserialized.tidspunkt,
                periode = periode,
                grunnlag = grunnlag,
                barn = Barn(fødselsdato = deserialized.barn.fødselsdato, identitetsnummer = deserialized.barn.identitetsnummer),
                søkersIdentitetsnummer = deserialized.søker.identitetsnummer
            ) to Behov(uløsteBehov = uløsteBehov, løsteBehov = setOf(vurdertPeriode))
        }
    }
}