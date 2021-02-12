package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarn
import no.nav.omsorgsdager.kronisksyktbarn.dto.LøsKroniskSyktBarnBehov
import no.nav.omsorgsdager.kronisksyktbarn.dto.OpprettKroniskSyktBarn
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime
import java.util.*

internal class KroniskSyktBarnOperasjoner(
    private val kroniskSyktBarnRepository: KroniskSyktBarnRepository
) : BehandlingOperasjoner<KroniskSyktBarnVedtak> {

    override suspend fun hent(behandlingId: BehandlingId) = kroniskSyktBarnRepository.hent(behandlingId)

    override suspend fun hentAlle(saksnummer: Saksnummer) = kroniskSyktBarnRepository.hentAlle(saksnummer)

    override suspend fun preOpprett(grunlag: Json): Set<Identitetsnummer> {
        return grunlag.deserialize<OpprettKroniskSyktBarn.Grunnlag>().involverteIdentitetsnummer
    }

    override suspend fun opprett(grunnlag: Json, correlationId: CorrelationId): Behandling<KroniskSyktBarnVedtak> {

        val (vedtak, behov) = KroniskSyktBarnVedtak.fraGrunnlag(grunnlag)
        val omsorgspengerSaksnummer = "${UUID.randomUUID()}" // TODO: Integrasjon for å hente saksnummeret

        return kroniskSyktBarnRepository.lagre(
            omsorgspengerSaksnummer = omsorgspengerSaksnummer,
            behandling = Behandling(
                vedtak = vedtak,
                behov = behov
            )
        )
    }

    override suspend fun løsninger(behandlingId: BehandlingId, grunnlag: Json): Behandling<KroniskSyktBarnVedtak> {
        val løsteBehov = grunnlag.deserialize<LøsKroniskSyktBarnBehov.Request>().løsteBehov
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