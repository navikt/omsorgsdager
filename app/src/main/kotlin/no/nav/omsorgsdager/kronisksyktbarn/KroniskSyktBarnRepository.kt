package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.aksjonspunkt.LøstAksjonpunkt
import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal interface KroniskSyktBarnRepository {
    fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>)
    fun fastsett(behandlingId: BehandlingId)
    fun deaktiver(behandlingId: BehandlingId)
    fun løsteAksjonspunkter(behandlingId: BehandlingId, løsteAksjonspunkter: Set<LøstAksjonpunkt>)
}

internal class InMemoryKroniskSyktBarnRespository : KroniskSyktBarnRepository {
    private val map = mutableMapOf<BehandlingId, Pair<KroniskSyktBarnVedtak, Aksjonspunkter>>()

    override fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>) {
        map[vedtak.behandlingId] = vedtak to Aksjonspunkter(
            uløsteAksjonspunkter = uløsteAksjonspunkter,
            løsteAksjonspunkter = emptySet()
        )
    }


    override fun fastsett(behandlingId: BehandlingId) {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        require(aksjonspunkter.løsteAksjonspunkter.isEmpty()) { "Må ha løst alle aksjonspunkter for å fastsette" }
        map[behandlingId] = vedtak.copy(
            status = VedtakStatus.FASTSATT,
            statusSistEndret = ZonedDateTime.now()
        ) to aksjonspunkter
    }

    override fun deaktiver(behandlingId: BehandlingId) {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        map[behandlingId] = vedtak.copy(
            status = VedtakStatus.DEAKTIVERT,
            statusSistEndret = ZonedDateTime.now()
        ) to aksjonspunkter
    }

    override fun løsteAksjonspunkter(behandlingId: BehandlingId, løsteAksjonspunkter: Set<LøstAksjonpunkt>) {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        val nyeLøsninger = løsteAksjonspunkter.map { it.navn() }

        val oppdaterteLøsteAksjonspunkter = aksjonspunkter.løsteAksjonspunkter.filterNot {
            it.navn() in nyeLøsninger
        }.plus(løsteAksjonspunkter).toSet()

        val alleLøsninger = oppdaterteLøsteAksjonspunkter.map { it.navn() }

        val oppdaterteAksjonspunkter = Aksjonspunkter(
            løsteAksjonspunkter = oppdaterteLøsteAksjonspunkter,
            uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter.filterNot {
                it.navn in alleLøsninger
            }.toSet()
        )

        map[behandlingId] = vedtak to oppdaterteAksjonspunkter
    }

}