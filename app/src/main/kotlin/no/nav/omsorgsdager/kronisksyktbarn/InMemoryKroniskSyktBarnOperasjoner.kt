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

internal class InMemoryKroniskSyktBarnOperasjoner : BehandlingOperasjoner<KroniskSyktBarnVedtak> {

    private val map = mutableMapOf<BehandlingId, Behandling<KroniskSyktBarnVedtak>>()

    override suspend fun hent(behandlingId: BehandlingId): Behandling<KroniskSyktBarnVedtak>? {
        return map[behandlingId]
    }

    override suspend fun hentAlle(saksnummer: Saksnummer): List<Behandling<KroniskSyktBarnVedtak>> {
        return map.filterValues { it.vedtak.saksnummer == saksnummer }.values.toList()
    }

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

        map[grunnlag.behandlingId] = Behandling(
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

        return map.getValue(grunnlag.behandlingId)
    }


    override suspend fun løsninger(behandlingId: BehandlingId, request: ObjectNode): Behandling<KroniskSyktBarnVedtak> {
        val behandling = map.getValue(behandlingId)
        val løsteBehov = request.map<LøsKroniskSyktBarnBehov.Request>().løsteBehov
        val løsteBehovNavn = løsteBehov.map { it.navn }

        val oppdaterteLøsteBehov = behandling.behov.løsteBehov.filterNot {
            it.navn in løsteBehovNavn
        }.plus(løsteBehov).toSet()

        val alleLøsninger = oppdaterteLøsteBehov.map { it.navn }

        val oppdaterteBehov = Behov(
            løsteBehov = oppdaterteLøsteBehov,
            uløsteBehov = behandling.behov.uløsteBehov.filterNot {
                it.navn in alleLøsninger
            }.toSet()
        )
        map[behandlingId] = behandling.copy(
            behov = oppdaterteBehov
        )
        return map.getValue(behandlingId)
    }

    override suspend fun innvilg(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> =
        endreStatus(behandlingId, VedtakStatus.INNVILGET, tidspunkt)

    override suspend fun avslå(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> =
        endreStatus(behandlingId, VedtakStatus.AVSLÅTT, tidspunkt)

    override suspend fun forkast(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> =
        endreStatus(behandlingId, VedtakStatus.FORKASTET, tidspunkt)

    override fun behandlingDto(behandling: Behandling<KroniskSyktBarnVedtak>): HentKroniskSyktBarn.Response =
        HentKroniskSyktBarn.Response(behandling)

    private fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> {
        val behandling = map.getValue(behandlingId)
        require(behandling.vedtak.status == VedtakStatus.FORESLÅTT) { "Må være i status FORESLÅTT for å kunne sette til $status" }
        if (status == VedtakStatus.INNVILGET) {
            require(behandling.kanInnvilges) { "Kan ikke ikke innvilges" }
        }

        map[behandlingId] = behandling.copy(
            vedtak = behandling.vedtak.copy(
                status = status,
                statusSistEndret = tidspunkt
            )
        )

        return map.getValue(behandlingId)
    }
}