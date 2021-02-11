package no.nav.omsorgsdager.kronisksyktbarn

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.SerDes.map
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.UløstBehov
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarn
import no.nav.omsorgsdager.kronisksyktbarn.dto.LøsKroniskSyktBarnBehov
import no.nav.omsorgsdager.kronisksyktbarn.dto.OpprettKroniskSyktBarn
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import no.nav.omsorgsdager.tid.Periode.Companion.utcNå
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal class KroniskSyktBarnOperasjoner(
    private val kroniskSyktBarnRepository: KroniskSyktBarnRepository
) : BehandlingOperasjoner<KroniskSyktBarnVedtak> {

    override suspend fun hent(behandlingId: BehandlingId) = kroniskSyktBarnRepository.hent(behandlingId)

    override suspend fun hentAlle(saksnummer: Saksnummer) = kroniskSyktBarnRepository.hentAlle(saksnummer)

    override suspend fun preOpprett(request: ObjectNode): Set<Identitetsnummer> {
        val grunnlag = request.map<OpprettKroniskSyktBarn.Grunnlag>()
        return grunnlag.involverteIdentitetsnummer
    }

    override suspend fun opprett(request: ObjectNode, correlationId: CorrelationId): Behandling<KroniskSyktBarnVedtak> {
        val grunnlag = request.map<OpprettKroniskSyktBarn.Grunnlag>()

        val tilOgMed = minOf(
            grunnlag.barn.fødselsdato.sisteDagIÅretOm18År(),
            grunnlag.søker.sisteDagSøkerHarRettTilOmsorgsdager
        )

        val behandling = Behandling(
            vedtak = KroniskSyktBarnVedtak(
                saksnummer = grunnlag.saksnummer,
                behandlingId = grunnlag.behandlingId,
                søker = grunnlag.søker,
                barn = grunnlag.barn,
                status = VedtakStatus.FORESLÅTT,
                statusSistEndret = utcNå(),
                periode = Periode(
                    fom = grunnlag.søknadMottatt.toLocalDateOslo(),
                    tom = tilOgMed
                )
            ),
            behov = Behov(
                uløsteBehov = setOf(UløstBehov("LEGEERKLÆRING")),
                løsteBehov = emptySet()
            )
        )

        return kroniskSyktBarnRepository.lagre(behandling)
    }

    override suspend fun løsninger(behandlingId: BehandlingId, request: ObjectNode): Behandling<KroniskSyktBarnVedtak> {
        val løsteBehov = request.map<LøsKroniskSyktBarnBehov.Request>().løsteBehov
        return kroniskSyktBarnRepository.leggTilLøsteBehov(behandlingId, løsteBehov)
    }

    override suspend fun innvilg(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> =
        kroniskSyktBarnRepository.endreStatus(behandlingId, VedtakStatus.INNVILGET, tidspunkt)

    override suspend fun avslå(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> =
        kroniskSyktBarnRepository.endreStatus(behandlingId, VedtakStatus.AVSLÅTT, tidspunkt)

    override suspend fun forkast(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> =
        kroniskSyktBarnRepository.endreStatus(behandlingId, VedtakStatus.FORKASTET, tidspunkt)

    override fun behandlingDto(behandling: Behandling<KroniskSyktBarnVedtak>): HentKroniskSyktBarn.Response =
        HentKroniskSyktBarn.Response(behandling)
}