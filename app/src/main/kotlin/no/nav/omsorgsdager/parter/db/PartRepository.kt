package no.nav.omsorgsdager.parter.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.parter.*
import no.nav.omsorgsdager.parter.Barn
import no.nav.omsorgsdager.parter.Involvering
import no.nav.omsorgsdager.parter.Motpart
import no.nav.omsorgsdager.parter.Søker
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class PartRepository(
    private val dataSource: DataSource) {

    internal fun hentParter(behandlingIder: List<BehandlingId>) : List<DbPart> {
        return emptyList()
    }

    internal fun hentInvolveringer(omsorgspengerSaksnummer: OmsorgspengerSaksnummer) : Map<Involvering, List<BehandlingId>> {
        return emptyMap()
    }

    internal fun hentOmsorgspengerSaksnummer(identitetsnummer: Identitetsnummer) : OmsorgspengerSaksnummer? {
        return null
    }

    internal companion object {
        internal fun TransactionalSession.leggTilParter(
            behandlingId: BehandlingId,
            parter: List<Part>) {
            val queries = parter.map { part -> when (part) {
                is Barn -> queryOf(
                    statement = LagreBarnStatement,
                    paramMap = mapOf(
                        "behandlingId" to behandlingId,
                        "identitetsnummer" to "${part.identitetsnummer}",
                        "fodselsdato" to part.fødselsdato
                    )
                )
                is Søker -> queryOf(
                    statement = LagrePersonStatement,
                    paramMap = mapOf(
                        "behandlingId" to behandlingId,
                        "identitetsnummer" to "${part.identitetsnummer}",
                        "omsorgspengerSaksnummer" to "${part.omsorgspengerSaksnummer}",
                        "type" to "SØKER"
                    )
                )
                is Motpart -> queryOf(
                    statement = LagrePersonStatement,
                    paramMap = mapOf(
                        "behandlingId" to behandlingId,
                        "identitetsnummer" to "${part.identitetsnummer}",
                        "omsorgspengerSaksnummer" to "${part.omsorgspengerSaksnummer}",
                        "type" to "MOTPART"
                    )
                )
                else -> throw IllegalStateException("Støtter ikke å lagre part av type ${part.javaClass}")
            }}
            queries.forEach { query -> update(query) }
        }

        @Language("PostgreSQL")
        private const val LagreBarnStatement = """
            INSERT INTO part (behandling_id, identitetsnummer, fodselsdato, type)
            VALUES(:behandlingId, :identitetsnummer, :fodselsdato, 'BARN')
        """

        @Language("PostgreSQL")
        private const val LagrePersonStatement = """
            INSERT INTO part (behandling_id, identitetsnummer, omsorgspenger_saksnummer, type)
            VALUES(:behandlingId, :identitetsnummer, :omsorgspengerSaksnummer, :type)
        """
    }
}