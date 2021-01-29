package no.nav.omsorgsdager.utvidetrett

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

internal class UtvidettRepository(
    private val dataSource: DataSource
) {

    internal fun hentBehandling(behandlingsId: String): List<String> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                hentQuery(
                    behandlingsId = behandlingsId,
                ).map { row -> row.toString() }.asList
            )
        }
    }

    internal fun lagre(behandlingsId: String, behandling: String) {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                lagreQuery(
                    behandling = behandling,
                    behandlingsId = behandlingsId
                ).asUpdate
            )
        }
    }

    internal fun oppdatere(behandlingsId: String, behandling: String) {

    }

    private companion object {
        private const val HentStatement = """
            SELECT * FROM behandling 
            WHERE behandlings_id = :behandlingsId
        """

        private fun hentQuery(
            behandlingsId: String
        ) = queryOf(
            HentStatement, mapOf("behandlingsId" to behandlingsId)
        )

        private const val LagreStatement = """
            INSERT INTO behandling 
                (behandlings_id, behandling)
            VALUES
                (:behandlings_id, (to_json(:behandling::json)))
        """

        private fun lagreQuery(
            behandlingsId: String,
            behandling: String
        ) = queryOf(
            LagreStatement, mapOf(
                "behandlings_id" to behandlingsId,
                "behandling" to behandling
            )
        )

        private const val OppdatereStatement = """
            UPDATE behandling
            SET behandling = :behandling
            WHERE behandlings_id = :behandlingsId
        """

        private fun oppdaterQuery(
            behandlingsId: String,
            behandling: String
        ) = queryOf(
            OppdatereStatement, mapOf(
                "behandlingsId" to behandlingsId,
                "behandling" to behandling
            )
        )
    }
}