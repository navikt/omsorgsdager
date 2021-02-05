package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.behov.UløstBehov
import no.nav.omsorgsdager.behov.kanInnvilges
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime

internal interface KroniskSyktBarnRepository {
    fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteBehov: Set<UløstBehov>) : Pair<KroniskSyktBarnVedtak, Behov>
    fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime) : Pair<KroniskSyktBarnVedtak, Behov>
    fun løsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>) : Pair<KroniskSyktBarnVedtak, Behov>
    fun hent(behandlingId: BehandlingId) : Pair<KroniskSyktBarnVedtak, Behov>?
    fun hentAlle(saksnummer: Saksnummer) : List<Pair<KroniskSyktBarnVedtak, Behov>>
}

internal class InMemoryKroniskSyktBarnRespository : KroniskSyktBarnRepository {
    private val map = mutableMapOf<BehandlingId, Pair<KroniskSyktBarnVedtak, Behov>>()

    override fun nyttVedtak(vedtak: KroniskSyktBarnVedtak, uløsteBehov: Set<UløstBehov>) : Pair<KroniskSyktBarnVedtak, Behov>{
        map[vedtak.behandlingId] = vedtak to Behov(
            uløsteBehov = uløsteBehov,
            løsteBehov = emptySet()
        )
        return map.getValue(vedtak.behandlingId)
    }

    override fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Pair<KroniskSyktBarnVedtak, Behov> {
        val (vedtak, behov) = map.getValue(behandlingId)
        require(vedtak.status == VedtakStatus.FORESLÅTT) { "Må være i status FORESLÅTT for å kunne sette til $status" }
        if (status == VedtakStatus.INNVILGET) {
            require(behov.uløsteBehov.isEmpty()) { "Må ha løst alle behov for å innvilge" }
            require(behov.løsteBehov.kanInnvilges()) { "Alle løsninger på behov må indikere at det kan innvilges"}
        }
        map[behandlingId] = vedtak.copy(
            status = status,
            statusSistEndret = tidspunkt
        ) to behov
        return map.getValue(behandlingId)
    }

    override fun løsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>) : Pair<KroniskSyktBarnVedtak, Behov> {
        val (vedtak, behov) = map.getValue(behandlingId)
        val nyeLøsninger = løsteBehov.map { it.navn }

        val oppdaterteLøsteBehov = behov.løsteBehov.filterNot {
            it.navn in nyeLøsninger
        }.plus(løsteBehov).toSet()

        val alleLøsninger = oppdaterteLøsteBehov.map { it.navn }

        val oppdaterteBehov = Behov(
            løsteBehov = oppdaterteLøsteBehov,
            uløsteBehov = behov.uløsteBehov.filterNot {
                it.navn in alleLøsninger
            }.toSet()
        )
        map[behandlingId] = vedtak to oppdaterteBehov
        return map.getValue(behandlingId)
    }

    override fun hent(behandlingId: BehandlingId): Pair<KroniskSyktBarnVedtak, Behov>? =
        map[behandlingId]

    override fun hentAlle(saksnummer: Saksnummer): List<Pair<KroniskSyktBarnVedtak, Behov>> {
        return map.filterValues { it.first.saksnummer == saksnummer }.values.toList()
    }
}