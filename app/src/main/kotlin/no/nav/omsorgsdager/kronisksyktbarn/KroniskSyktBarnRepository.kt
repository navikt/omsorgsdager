package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal interface KroniskSyktBarnRepository {
    fun hent(behandlingId: BehandlingId): Behandling<KroniskSyktBarnVedtak>?
    fun hentAlle(saksnummer: Saksnummer) : List<Behandling<KroniskSyktBarnVedtak>>
    fun lagre(behandling: Behandling<KroniskSyktBarnVedtak>) : Behandling<KroniskSyktBarnVedtak>
    fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak>
    fun leggTilLøsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>): Behandling<KroniskSyktBarnVedtak>
}

internal class InMemoryKroniskSyktBarnRepository : KroniskSyktBarnRepository {
    private val map = mutableMapOf<BehandlingId, Behandling<KroniskSyktBarnVedtak>>()

    override fun hent(behandlingId: BehandlingId): Behandling<KroniskSyktBarnVedtak>? =
        map[behandlingId]

    override fun hentAlle(saksnummer: Saksnummer): List<Behandling<KroniskSyktBarnVedtak>> =
        map.filterValues { it.vedtak.saksnummer == saksnummer }.values.toList()


    override fun lagre(behandling: Behandling<KroniskSyktBarnVedtak>): Behandling<KroniskSyktBarnVedtak> {
        map[behandling.vedtak.behandlingId] = behandling
        return map.getValue(behandling.vedtak.behandlingId)
    }

    override fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> {
        val current = map.getValue(behandlingId)
        map[behandlingId] = current.copy(
            vedtak = current.vedtak.copy(status = status, statusSistEndret = tidspunkt)
        )
        return map.getValue(behandlingId)
    }

    override fun leggTilLøsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>): Behandling<KroniskSyktBarnVedtak> {
        val behandling = map.getValue(behandlingId)

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
}

internal class DbKroniskSyktBarnRepository : KroniskSyktBarnRepository {
    override fun hent(behandlingId: BehandlingId): Behandling<KroniskSyktBarnVedtak>? {
        TODO("Not yet implemented")
    }

    override fun hentAlle(saksnummer: Saksnummer): List<Behandling<KroniskSyktBarnVedtak>> {
        TODO("Not yet implemented")
    }

    override fun lagre(behandling: Behandling<KroniskSyktBarnVedtak>): Behandling<KroniskSyktBarnVedtak> {
        TODO("Not yet implemented")
    }

    override fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> {
        TODO("Not yet implemented")
    }

    override fun leggTilLøsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>): Behandling<KroniskSyktBarnVedtak> {
        TODO("Not yet implemented")
    }

}