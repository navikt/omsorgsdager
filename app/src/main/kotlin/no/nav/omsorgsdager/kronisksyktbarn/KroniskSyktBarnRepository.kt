package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.aksjonspunkt.LøstAksjonpunkt
import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt
import no.nav.omsorgsdager.aksjonspunkt.kanInnvilges
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal interface KroniskSyktBarnRepository {
    fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun løsteAksjonspunkter(behandlingId: BehandlingId, løsteAksjonspunkter: Set<LøstAksjonpunkt>) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun hent(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>?
    fun hentAlle(saksnummer: Saksnummer) : List<Pair<KroniskSyktBarnVedtak, Aksjonspunkter>>
}

internal class InMemoryKroniskSyktBarnRespository : KroniskSyktBarnRepository {
    private val map = mutableMapOf<BehandlingId, Pair<KroniskSyktBarnVedtak, Aksjonspunkter>>()

    override fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>{
        map[vedtak.behandlingId] = vedtak to Aksjonspunkter(
            uløsteAksjonspunkter = uløsteAksjonspunkter,
            løsteAksjonspunkter = emptySet()
        )
        return map.getValue(vedtak.behandlingId)
    }

    override fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Pair<KroniskSyktBarnVedtak, Aksjonspunkter> {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        require(vedtak.status == VedtakStatus.FORESLÅTT) { "Må være i status FORESLÅTT for å kunne sette til $status" }
        if (status == VedtakStatus.INNVILGET) {
            require(aksjonspunkter.uløsteAksjonspunkter.isEmpty()) { "Må ha løst alle aksjonspunkter for å innvilge" }
            require(aksjonspunkter.løsteAksjonspunkter.kanInnvilges()) { "Alle løsninger på aksjonspunkt må indikere at det kan innvilges"}
        }
        map[behandlingId] = vedtak.copy(
            status = status,
            statusSistEndret = tidspunkt
        ) to aksjonspunkter
        return map.getValue(behandlingId)
    }

    override fun løsteAksjonspunkter(behandlingId: BehandlingId, løsteAksjonspunkter: Set<LøstAksjonpunkt>) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter> {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        val nyeLøsninger = løsteAksjonspunkter.map { it.navn }

        val oppdaterteLøsteAksjonspunkter = aksjonspunkter.løsteAksjonspunkter.filterNot {
            it.navn in nyeLøsninger
        }.plus(løsteAksjonspunkter).toSet()

        val alleLøsninger = oppdaterteLøsteAksjonspunkter.map { it.navn }

        val oppdaterteAksjonspunkter = Aksjonspunkter(
            løsteAksjonspunkter = oppdaterteLøsteAksjonspunkter,
            uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter.filterNot {
                it.navn in alleLøsninger
            }.toSet()
        )
        map[behandlingId] = vedtak to oppdaterteAksjonspunkter
        return map.getValue(behandlingId)
    }

    override fun hent(behandlingId: BehandlingId): Pair<KroniskSyktBarnVedtak, Aksjonspunkter>? =
        map[behandlingId]

    override fun hentAlle(saksnummer: Saksnummer): List<Pair<KroniskSyktBarnVedtak, Aksjonspunkter>> {
        return map.filterValues { it.first.saksnummer == saksnummer }.values.toList()
    }
}