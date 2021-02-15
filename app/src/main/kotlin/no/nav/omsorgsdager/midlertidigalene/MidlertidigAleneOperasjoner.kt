package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.midlertidigalene.dto.LøsMidlertidigAleneBehov
import no.nav.omsorgsdager.midlertidigalene.dto.OpprettMidlertidigAlene
import no.nav.omsorgsdager.vedtak.VedtakRepository
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime
import java.util.*

internal class MidlertidigAleneOperasjoner(
    private val midlertidigAleneRepository: VedtakRepository<MidlertidigAleneVedtak>
) : BehandlingOperasjoner<MidlertidigAleneVedtak> {
    override suspend fun hent(behandlingId: BehandlingId): Behandling<MidlertidigAleneVedtak>? =
        midlertidigAleneRepository.hent(behandlingId)

    override suspend fun hentAlle(saksnummer: Saksnummer): List<Behandling<MidlertidigAleneVedtak>> =
        midlertidigAleneRepository.hentAlle(saksnummer)

    override suspend fun preOpprett(grunnlag: Json): Set<Identitetsnummer> =
        grunnlag.deserialize<OpprettMidlertidigAlene.Grunnlag>().involverteIdentitetsnummer


    override suspend fun opprett(grunnlag: Json, correlationId: CorrelationId): Behandling<MidlertidigAleneVedtak> {
        val (vedtak, behov) = MidlertidigAleneVedtak.fraGrunnlag(grunnlag)
        val omsorgspengerSaksnummer = "${UUID.randomUUID()}" // TODO: Integrasjon for å hente saksnummeret

        return midlertidigAleneRepository.lagre(
            omsorgspengerSaksnummer = omsorgspengerSaksnummer,
            behandling = Behandling(
                vedtak = vedtak,
                behov = behov
            )
        )
    }

    override suspend fun løsninger(behandlingId: BehandlingId, grunnlag: Json): Behandling<MidlertidigAleneVedtak> {
        val løsteBehov = grunnlag.deserialize<LøsMidlertidigAleneBehov.Request>()

        val behandling = midlertidigAleneRepository.leggTilLøsteBehov(
            behandlingId = behandlingId,
            løsteBehov = løsteBehov.løsteBehov
        )

        // TODO: Denne logikken må flyttes til repository
        return when (løsteBehov.VURDERE_MIDLERTIDIG_ALENE?.periode != null) {
            true -> midlertidigAleneRepository.lagre(
                omsorgspengerSaksnummer = "${UUID.randomUUID()}", // TODO: Integrasjon for å hente saksnummeret
                behandling = behandling.copy(
                    vedtak = behandling.vedtak.copy(periode = løsteBehov.VURDERE_MIDLERTIDIG_ALENE!!.periode!!)
                )
            )
            false -> behandling
        }
    }

    override suspend fun innvilg(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<MidlertidigAleneVedtak> =
        midlertidigAleneRepository.endreStatus(behandlingId, VedtakStatus.INNVILGET, tidspunkt)

    override suspend fun avslå(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<MidlertidigAleneVedtak> =
        midlertidigAleneRepository.endreStatus(behandlingId, VedtakStatus.AVSLÅTT, tidspunkt)

    override suspend fun forkast(behandlingId: BehandlingId, tidspunkt: ZonedDateTime): Behandling<MidlertidigAleneVedtak> =
        midlertidigAleneRepository.endreStatus(behandlingId, VedtakStatus.FORKASTET, tidspunkt)

}