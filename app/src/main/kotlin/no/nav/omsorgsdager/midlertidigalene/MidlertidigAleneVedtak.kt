package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.somUløstBehov
import no.nav.omsorgsdager.midlertidigalene.dto.OpprettMidlertidigAlene
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal data class MidlertidigAleneVedtak(
    override val saksnummer: Saksnummer,
    override val behandlingId: BehandlingId,
    override val status: VedtakStatus,
    override val statusSistEndret: ZonedDateTime,
    override val periode: Periode?,
    override val grunnlag: Json,
    internal val søkersIdentitetsnummer: Identitetsnummer,
    internal val motpartsIdentitetsnummer: Identitetsnummer) : Vedtak {
    override val etGjeldendeVedtakPer: Saksnummer = saksnummer

    override fun kopiMedNyPeriode(nyPeriode: Periode) = copy(
        periode = nyPeriode
    )
    override val involverteIdentitetsnummer = setOf(søkersIdentitetsnummer, motpartsIdentitetsnummer)


    internal companion object {
        internal fun fraGrunnlag(grunnlag: Json) : Pair<MidlertidigAleneVedtak, Behov> {
            val deserialized = grunnlag.deserialize<OpprettMidlertidigAlene.Grunnlag>()

            val uløsteBehov = setOf("VURDERE_MIDLERTIDIG_ALENE".somUløstBehov())

            return MidlertidigAleneVedtak(
                saksnummer = deserialized.saksnummer,
                behandlingId = deserialized.behandlingId,
                status = VedtakStatus.FORESLÅTT,
                statusSistEndret = deserialized.tidspunkt,
                periode = null, // For midlertidig alene er periode en av de tingene som skal løses manuelt
                grunnlag = grunnlag,
                motpartsIdentitetsnummer = deserialized.motpart.identitetsnummer,
                søkersIdentitetsnummer = deserialized.søker.identitetsnummer
            ) to Behov(uløsteBehov = uløsteBehov, løsteBehov = emptySet())
        }
    }
}