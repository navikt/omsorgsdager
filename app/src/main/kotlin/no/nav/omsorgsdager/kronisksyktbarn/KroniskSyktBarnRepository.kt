package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.aksjonspunkt.LøstAksjonpunkt
import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal interface KroniskSyktBarnRepository {
    fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>)
    fun fastsett(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun deaktiver(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun løsteAksjonspunkter(behandlingId: BehandlingId, løsteAksjonspunkter: Set<LøstAksjonpunkt>) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun hent(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>?
    fun hentAlle(saksnummer: Saksnummer) : List<Pair<KroniskSyktBarnVedtak, Aksjonspunkter>>
}

internal class InMemoryKroniskSyktBarnRespository : KroniskSyktBarnRepository {
    private val map = mutableMapOf<BehandlingId, Pair<KroniskSyktBarnVedtak, Aksjonspunkter>>()

    override fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>) {
        map[vedtak.behandlingId] = vedtak to Aksjonspunkter(
            uløsteAksjonspunkter = uløsteAksjonspunkter,
            løsteAksjonspunkter = emptySet()
        )
    }

    override fun fastsett(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter> {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        require(aksjonspunkter.uløsteAksjonspunkter.isEmpty()) { "Må ha løst alle aksjonspunkter for å fastsette" }
        require(vedtak.status == VedtakStatus.FORSLAG) { "Må være i status FORSLAG for å kunne sette til FASTSATT" }
        map[behandlingId] = vedtak.copy(
            status = VedtakStatus.FASTSATT,
            statusSistEndret = ZonedDateTime.now()
        ) to aksjonspunkter
        return map.getValue(behandlingId)
    }

    override fun deaktiver(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter> {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        require(vedtak.status == VedtakStatus.FORSLAG) { "Må være i status FORSLAG for å kunne sette til DEAKTIVERT" }
        map[behandlingId] = vedtak.copy(
            status = VedtakStatus.DEAKTIVERT,
            statusSistEndret = ZonedDateTime.now()
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