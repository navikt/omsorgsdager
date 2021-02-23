package no.nav.omsorgsdager.behandling.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.behandling.NyBehandling
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class BehandlingRepository(
    private val dataSource: DataSource) {

    internal fun hentEn(behandlingId: K9BehandlingId) : DbBehandling? {
        return null
    }

    internal fun hentAlle(saksnummer: K9Saksnummer) : List<DbBehandling> {
        return emptyList()
    }

    internal fun hentAlle(behandlingIder: List<BehandlingId>) : List<DbBehandling> {
        return emptyList()
    }

    internal companion object {
        internal fun TransactionalSession.lagreBehandling(behandling: NyBehandling) : BehandlingId {
            val nyBehandlingId = updateAndReturnGeneratedKey(queryOf(
                statement = LagreBehandlingStatement,
                paramMap = mapOf(
                    "saksnummer" to "${behandling.saksnummer}",
                    "behandlingId" to "${behandling.behandlingId}",
                    "status" to behandling.status.name,
                    "type" to behandling.type.name,
                    "tidspunkt" to behandling.tidspunkt,
                    "fom" to behandling.periode.fom,
                    "tom" to behandling.periode.tom,
                    "grunnlag" to behandling.grunnlag.raw
                )
            ))
            return requireNotNull(nyBehandlingId) // TODO
        }

        @Language("PostgreSQL")
        private const val LagreBehandlingStatement = """
            INSERT INTO behandling (k9_saksnummer, k9_behandling_id, status, type, tidspunkt, fom, tom, grunnlag)
            VALUES(:saksnummer, :behandlingId, :status, :type, :tidspunkt, :fom, :tom, :grunnlag ::jsonb)
            ON CONFLICT (k9_behandling_id) DO NOTHING 
        """
    }
}
/*
    k9_saksnummer               VARCHAR(50) NOT NULL,
    k9_behandling_id            VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    type                        VARCHAR(50) NOT NULL,
    tidspunkt                   TIMESTAMP WITH TIME ZONE NOT NULL,
    fom                         DATE NOT NULL,
    tom                         DATE NOT NULL,
    grunnlag                    JSONB NOT NULL,
    UNIQUE (k9_behandling_id, type)
 */