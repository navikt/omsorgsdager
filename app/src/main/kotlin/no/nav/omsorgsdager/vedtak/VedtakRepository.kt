package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnVedtak
import no.nav.omsorgsdager.midlertidigalene.MidlertidigAleneVedtak
import java.time.ZonedDateTime

internal interface VedtakRepository<V: Vedtak> {
    fun hent(behandlingId: BehandlingId): Behandling<V>?
    fun hentAlle(saksnummer: Saksnummer) : List<Behandling<V>>
    fun lagre(behandling: Behandling<V>, omsorgspengerSaksnummer: Saksnummer) : Behandling<V>
    fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<V>
    fun leggTilLøsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>): Behandling<V>
}

// TODO: Denne kan fjernes når Db-Repositories er tatt i bruk
internal class InMemoryVedtakRepository<V: Vedtak> : VedtakRepository<V> {
    private val map = mutableMapOf<BehandlingId, Behandling<V>>()

    override fun hent(behandlingId: BehandlingId): Behandling<V>? =
        map[behandlingId]

    override fun hentAlle(saksnummer: Saksnummer): List<Behandling<V>> =
        map.filterValues { it.vedtak.saksnummer == saksnummer }.values.toList()


    override fun lagre(behandling: Behandling<V>, omsorgspengerSaksnummer: Saksnummer): Behandling<V> {
        map[behandling.vedtak.behandlingId] = behandling
        return map.getValue(behandling.vedtak.behandlingId)
    }

    override fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<V> {
        val current = map.getValue(behandlingId)
        val vedtakMedOppdatertStatus = when (current.vedtak) {
            is KroniskSyktBarnVedtak -> current.vedtak.copy(status = status, statusSistEndret = tidspunkt)
            is MidlertidigAleneVedtak -> current.vedtak.copy(status = status, statusSistEndret = tidspunkt)
            else -> throw IllegalStateException("Støtter ikke ${current.vedtak.javaClass.simpleName}")
        }
        map[behandlingId] = current.copy(
            vedtak = vedtakMedOppdatertStatus as V
        )
        return map.getValue(behandlingId)
    }

    override fun leggTilLøsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>): Behandling<V> {
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
