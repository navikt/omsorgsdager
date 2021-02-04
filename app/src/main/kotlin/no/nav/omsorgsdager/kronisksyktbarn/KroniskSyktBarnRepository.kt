package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.aksjonspunkt.Aksjonspunkter
import no.nav.omsorgsdager.aksjonspunkt.LøstAksjonpunkt
import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal interface KroniskSyktBarnRepository {
    fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteAksjonspunkter: Set<UløstAksjonspunkt>) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun innvilg(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun avslå(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
    fun deaktiver(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter>
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

    override fun innvilg(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Aksjonspunkter> {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        require(aksjonspunkter.uløsteAksjonspunkter.isEmpty()) { "Må ha løst alle aksjonspunkter for å innvilge" }
        require(vedtak.status == VedtakStatus.FORSLAG) { "Må være i status FORSLAG for å kunne sette til INNVILGET" }
        map[behandlingId] = vedtak.copy(
            status = VedtakStatus.INNVILGET,
            statusSistEndret = ZonedDateTime.now()
        ) to aksjonspunkter
        return map.getValue(behandlingId)
    }

    override fun avslå(behandlingId: BehandlingId): Pair<KroniskSyktBarnVedtak, Aksjonspunkter> {
        val (vedtak, aksjonspunkter) = map.getValue(behandlingId)
        require(vedtak.status == VedtakStatus.FORSLAG) { "Må være i status FORSLAG for å kunne sette til AVSLÅTT" }
        map[behandlingId] = vedtak.copy(
            status = VedtakStatus.AVSLÅTT,
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