package no.nav.omsorgsdager.kronisksyktbarn

import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.BehovRepository.hentBehov
import no.nav.omsorgsdager.behov.BehovRepository.leggTilBehov
import no.nav.omsorgsdager.behov.BehovRepository.leggTilLøsteBehov
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.kronisksyktbarn.dto.Barn
import no.nav.omsorgsdager.kronisksyktbarn.dto.Søker
import no.nav.omsorgsdager.parter.ParterRepository
import no.nav.omsorgsdager.parter.ParterRepository.hentParter
import no.nav.omsorgsdager.parter.ParterRepository.leggTilParter
import no.nav.omsorgsdager.vedtak.VedtakRepository
import no.nav.omsorgsdager.vedtak.VedtakRepository.endreVedtakPeriode
import no.nav.omsorgsdager.vedtak.VedtakRepository.endreVedtakStatus
import no.nav.omsorgsdager.vedtak.VedtakRepository.hentAlleVedtak
import no.nav.omsorgsdager.vedtak.VedtakRepository.hentVedtak
import no.nav.omsorgsdager.vedtak.VedtakRepository.lagreVedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime
import javax.sql.DataSource

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

internal class DbKroniskSyktBarnRepository(
    private val dataSource: DataSource
) : KroniskSyktBarnRepository {
    override fun hent(behandlingId: BehandlingId): Behandling<KroniskSyktBarnVedtak>? {
         return using(sessionOf(dataSource)) { session ->
             val vedtak = session.hentVedtak(behandlingId)
             when (vedtak) {
                 null -> null
                 else -> {
                     val behov = session.hentBehov(vedtakId = vedtak.vedtakId)
                     val parter = session.hentParter(vedtakId = vedtak.vedtakId)
                     Triple(vedtak, behov, parter).tilBehandling()
                 }
             }
         }
    }

    override fun hentAlle(saksnummer: Saksnummer): List<Behandling<KroniskSyktBarnVedtak>> {
        // TODO: Optimer henting av behov og parter.
        return using(sessionOf(dataSource)) { session ->
            session.hentAlleVedtak(saksnummer).map {
                val behov = session.hentBehov(vedtakId = it.vedtakId)
                val parter = session.hentParter(vedtakId = it.vedtakId)
                Triple(it, behov, parter).tilBehandling()
            }
        }
    }

    override fun lagre(behandling: Behandling<KroniskSyktBarnVedtak>): Behandling<KroniskSyktBarnVedtak> {
        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                transactionalSession.lagreVedtak(VedtakRepository.LagreVedtak(
                    saksnummer = behandling.vedtak.saksnummer,
                    behandlingId = behandling.vedtak.behandlingId,
                    grunnlag = "{}".somJson(), // TODO
                    type = VedtakRepository.VedtakType.KRONISK_SYKT_BARN
                ))
                val vedtak = transactionalSession.endreVedtakPeriode(VedtakRepository.EndrePeriode(
                    behandlingId = behandling.vedtak.behandlingId,
                    periode = behandling.vedtak.periode
                ))
                val parter = transactionalSession.leggTilParter(
                    vedtakId = vedtak.vedtakId,
                    parter = listOf(
                        ParterRepository.Søker(
                            fødselsdato = behandling.vedtak.søker.fødselsdato,
                            identitetsnummer = behandling.vedtak.søker.identitetsnummer,
                            omsorgspengerSaksnummer = "TODO"
                        ),
                        ParterRepository.Barn(
                            fødselsdato = behandling.vedtak.barn.fødselsdato,
                            identitetsnummer = behandling.vedtak.barn.identitetsnummer
                        )
                    )
                )
                val behov = transactionalSession.leggTilBehov(
                    vedtakId = vedtak.vedtakId,
                    behov = behandling.behov
                )
                Triple(vedtak, behov, parter).tilBehandling()
            }
        }
    }

    override fun endreStatus(behandlingId: BehandlingId, status: VedtakStatus, tidspunkt: ZonedDateTime): Behandling<KroniskSyktBarnVedtak> {
        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                val vedtak = transactionalSession.endreVedtakStatus(VedtakRepository.EndreStatus(
                    behandlingId = behandlingId,
                    status = status,
                    tidspunkt = tidspunkt
                ))
                val behov = session.hentBehov(vedtakId = vedtak.vedtakId)
                val parter = session.hentParter(vedtakId = vedtak.vedtakId)
                Triple(vedtak, behov, parter).tilBehandling()
            }
        }
    }

    override fun leggTilLøsteBehov(behandlingId: BehandlingId, løsteBehov: Set<LøstBehov>): Behandling<KroniskSyktBarnVedtak> {
        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                val vedtak = requireNotNull(transactionalSession.hentVedtak(behandlingId = behandlingId)) {
                    "Finner ikke vedtak med behandlingId $behandlingId"
                }
                transactionalSession.leggTilLøsteBehov(vedtakId = vedtak.vedtakId, løsteBehov = løsteBehov)
                val behov = transactionalSession.hentBehov(vedtakId = vedtak.vedtakId)
                val parter = transactionalSession.hentParter(vedtakId = vedtak.vedtakId)
                Triple(vedtak, behov, parter).tilBehandling()
            }
        }
    }

    private fun Triple<VedtakRepository.DbVedtak, Behov, List<ParterRepository.Part>>.tilBehandling() : Behandling<KroniskSyktBarnVedtak> {
        val søker = third.first { it is ParterRepository.Søker }.let { it as ParterRepository.Søker }.let { Søker(
            identitetsnummer = it.identitetsnummer,
            fødselsdato = it.fødselsdato
        )}

        val barn = third.first { it is ParterRepository.Barn }.let { it as ParterRepository.Barn }.let { Barn(
            identitetsnummer = it.identitetsnummer,
            fødselsdato = it.fødselsdato,
            harSammeBosted = true // TODO
        )}

        val vedtak = KroniskSyktBarnVedtak(
            saksnummer = first.saksnummer,
            behandlingId = first.behandlingId,
            status = first.status,
            statusSistEndret = first.statusSistEndret,
            periode = requireNotNull(first.periode) {
                "KroniskSyktBarnVedtak må alltid ha en periode."
            },
            barn = barn,
            søker = søker
        )

        return Behandling(
            vedtak = vedtak,
            behov = second
        )
    }

}