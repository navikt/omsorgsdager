package no.nav.omsorgsdager.behandling

import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.behandling.db.BehandlingRepository
import no.nav.omsorgsdager.behandling.db.BehandlingRepository.Companion.lagreBehandling
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnBehandling
import no.nav.omsorgsdager.midlertidigalene.MidlertidigAleneBehandling
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.parter.Part
import no.nav.omsorgsdager.parter.db.PartRepository
import no.nav.omsorgsdager.parter.db.PartRepository.Companion.leggTilParter
import javax.sql.DataSource

internal class BehandlingService(
    private val dataSource: DataSource,
    private val behandlingRepository: BehandlingRepository,
    private val partRepository: PartRepository) {

    internal fun lagre(behandling: NyBehandling, parter: List<Part>) {
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session -> session.transaction { tx ->
            tx.lagreBehandling(behandling)?.also { nyBehandlingId ->
                tx.leggTilParter(behandlingId = nyBehandlingId, parter = parter)
            }
        }}
    }

    /**
     * Hente basert på en behandlingId i k9-sak
     */
    internal fun hentAlle(behandlingId: K9BehandlingId) : List<EksisterendeBehandling> {
        val dbBehandling = behandlingRepository.hentAlle(behandlingId)
        val parter = partRepository.hentParter(dbBehandling.map { it.id }).map { it.part }

        return dbBehandling.map { it.type.operasjoner.mapTilEksisterendeBehandling(
            dbBehandling = it,
            parter = parter
        )}
    }

    /**
     * Hente basert på et saksnummer i k9-sak og finne alle tilhørende behandlinger.
     */
    private fun hentAlle(saksnummer: K9Saksnummer) : List<EksisterendeBehandling> {
        val dbBehandlinger = behandlingRepository.hentAlle(saksnummer)

        val parter = partRepository.hentParter(dbBehandlinger.map { it.id })
            .groupBy { it.behandlingId }
            .mapValues { it -> it.value.map { it.part } }

        return dbBehandlinger.map { it.type.operasjoner.mapTilEksisterendeBehandling(
            dbBehandling = it,
            parter = parter.getOrDefault(it.id, emptyList())
        )}
    }

    internal fun hentAlleGjeldende(saksnummer: K9Saksnummer) : GjeldendeBehandlinger {
        val eksisterendeBehandlinger = hentAlle(saksnummer)
        return GjeldendeBehandlinger(
            alleKroniskSyktBarn = eksisterendeBehandlinger.filterIsInstance<KroniskSyktBarnBehandling>(),
            alleMidlertidigAlene = eksisterendeBehandlinger.filterIsInstance<MidlertidigAleneBehandling>()
        )
    }

    /**
     * Hente basert på et saksnummer i omsorgspenger-sak
     * Her finner man ikke bare behandlinger man selv vare søker
     * Men også der du er motpart i f.eks. Midlertidig alene.
     */
    private fun hentAlle(sakssnummer: OmsorgspengerSaksnummer) : Map<Involvering, List<EksisterendeBehandling>> {
        val involveringer = partRepository.hentInvolveringer(sakssnummer)
        val behandlingIder = involveringer.values.flatten()

        val dbBehandlinger = behandlingRepository.hentAlle(behandlingIder)

        val parter = partRepository.hentParter(dbBehandlinger.map { it.id })
            .groupBy { it.behandlingId }
            .mapValues { it -> it.value.map { it.part } }

        val eksisterendeBehandlinger = dbBehandlinger
            .associateBy { it.id }
            .mapValues { it.value.type.operasjoner.mapTilEksisterendeBehandling(
                dbBehandling = it.value,
                parter = parter.getOrDefault(it.value.id, emptyList())
            )}

        return involveringer
            .mapValues { it.value.map { behandlingId -> eksisterendeBehandlinger[behandlingId] } }
            .mapValues { it.value.filterNotNull() }
    }

    internal fun hentAlleGjeldende(saksnummer: OmsorgspengerSaksnummer) : Map<Involvering, GjeldendeBehandlinger> {
        return hentAlle(saksnummer).mapValues { GjeldendeBehandlinger(
            alleKroniskSyktBarn = it.value.filterIsInstance<KroniskSyktBarnBehandling>(),
            alleMidlertidigAlene = it.value.filterIsInstance<MidlertidigAleneBehandling>()
        )}
    }
}