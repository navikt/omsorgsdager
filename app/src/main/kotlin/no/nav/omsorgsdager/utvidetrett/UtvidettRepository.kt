package no.nav.omsorgsdager.utvidetrett

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

internal class UtvidettRepository(
    private val dataSource: DataSource) {

    internal fun hent(behandlingsId: String) : List<String> {
        return using(sessionOf(dataSource)) { session ->
            session.run(hentQuery(
                behandlingsId = behandlingsId,
            ).map { row -> row.toString() }.asList)
        }
    }

    private companion object {
        private const val HentStatement = """
            SELECT * FROM behandling 
            WHERE behandlingsId = :behandlingsId
        """

        private fun hentQuery(
            behandlingsId: String) = queryOf(HentStatement, mapOf(
            "behandlingsId" to behandlingsId
        ))
    }
}