package no.nav.omsorgsdager.kronisksyktbarn

import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.behov.BehovDbOperasjoner.hentBehov
import no.nav.omsorgsdager.behov.BehovDbOperasjoner.leggTilBehov
import no.nav.omsorgsdager.behov.BehovDbOperasjoner.leggTilLøsteBehov
import no.nav.omsorgsdager.behov.LøstBehov
import no.nav.omsorgsdager.parter.ParterDbOperasjoner
import no.nav.omsorgsdager.parter.ParterDbOperasjoner.hentParter
import no.nav.omsorgsdager.parter.ParterDbOperasjoner.leggTilParter
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner.endreVedtakPeriode
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner.endreVedtakStatus
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner.hentAlleVedtak
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner.hentVedtak
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner.lagreVedtak
import no.nav.omsorgsdager.vedtak.VedtakRepository
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.ZonedDateTime
import javax.sql.DataSource

internal class DbKroniskSyktBarnRepository(
    private val dataSource: DataSource
) : VedtakRepository<KroniskSyktBarnVedtak> {
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

    override fun lagre(behandling: Behandling<KroniskSyktBarnVedtak>, omsorgspengerSaksnummer: Saksnummer): Behandling<KroniskSyktBarnVedtak> {
        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                transactionalSession.lagreVedtak(VedtakDbOperasjoner.LagreVedtak(
                    saksnummer = behandling.vedtak.saksnummer,
                    behandlingId = behandling.vedtak.behandlingId,
                    grunnlag = behandling.vedtak.grunnlag,
                    type = VedtakDbOperasjoner.VedtakType.KRONISK_SYKT_BARN
                ))
                val vedtak = transactionalSession.endreVedtakPeriode(VedtakDbOperasjoner.EndrePeriode(
                    behandlingId = behandling.vedtak.behandlingId,
                    periode = behandling.vedtak.periode
                ))
                val parter = transactionalSession.leggTilParter(
                    vedtakId = vedtak.vedtakId,
                    parter = listOf(
                        ParterDbOperasjoner.Søker(
                            identitetsnummer = behandling.vedtak.søkersIdentitetsnummer,
                            omsorgspengerSaksnummer = omsorgspengerSaksnummer
                        ),
                        ParterDbOperasjoner.Barn(
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
                val vedtak = transactionalSession.endreVedtakStatus(VedtakDbOperasjoner.EndreStatus(
                    behandlingId = behandlingId,
                    status = status,
                    tidspunkt = tidspunkt
                ))
                val behov = transactionalSession.hentBehov(vedtakId = vedtak.vedtakId)
                val parter = transactionalSession.hentParter(vedtakId = vedtak.vedtakId)
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

    private fun Triple<VedtakDbOperasjoner.DbVedtak, Behov, List<ParterDbOperasjoner.Part>>.tilBehandling() : Behandling<KroniskSyktBarnVedtak> {
        val søkersIdentitetsnummer = third.first { it is ParterDbOperasjoner.Søker }.let { it as ParterDbOperasjoner.Søker }.identitetsnummer

        val barn = third.first { it is ParterDbOperasjoner.Barn }.let { it as ParterDbOperasjoner.Barn }.let { KroniskSyktBarnVedtak.Barn(
            identitetsnummer = it.identitetsnummer,
            fødselsdato = it.fødselsdato
        )}

        val vedtak = KroniskSyktBarnVedtak(
            saksnummer = first.saksnummer,
            behandlingId = first.behandlingId,
            status = first.status,
            statusSistEndret = first.statusSistEndret,
            periode = requireNotNull(first.periode) {
                "KroniskSyktBarnVedtak må alltid ha en periode."
            },
            søkersIdentitetsnummer = søkersIdentitetsnummer,
            barn = barn,
            grunnlag = first.grunnlag
        )

        return Behandling(
            vedtak = vedtak,
            behov = second
        )
    }

}