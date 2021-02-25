package no.nav.omsorgsdager.parter.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnumer
import no.nav.omsorgsdager.SecureLogger
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
        return sessionOf(dataSource).use { session ->
            val query = queryOf(
                statement = HenteParterStatement,
                paramMap = mapOf(
                    "behandlingIder" to session.createArrayOf("oid", behandlingIder)
                )
            )
            session.run(query.map { row ->
                val behandlingId = row.long("behandling_id")
                val type = row.string("type")
                when (type) {
                    "SØKER" -> DbPart(behandlingId = behandlingId, part = Søker(
                        identitetsnummer = row.string("identitetsnummer").somIdentitetsnummer(),
                        omsorgspengerSaksnummer = row.string("omsorgspenger_saksnummer").somOmsorgspengerSaksnumer()
                    ))
                    "MOTPART" -> DbPart(behandlingId = behandlingId, part = Motpart(
                        identitetsnummer = row.string("identitetsnummer").somIdentitetsnummer(),
                        omsorgspengerSaksnummer = row.string("omsorgspenger_saksnummer").somOmsorgspengerSaksnumer()
                    ))
                    "BARN" -> DbPart(behandlingId = behandlingId, part = Barn(
                        identitetsnummer = row.stringOrNull("identitetsnummer")?.somIdentitetsnummer(), // TODO: saksnummer også på barn... ?
                        fødselsdato = row.localDate("fodselsdato")
                    ))
                    else -> throw IllegalStateException("Ukjent Type=[$type], BehandlingId=[$behandlingId]")
                }

            }.asList)
        }
    }

    internal fun hentInvolveringer(omsorgspengerSaksnummer: OmsorgspengerSaksnummer) : Map<Involvering, List<BehandlingId>> {
        val query = queryOf(
            statement = HenteInvolveringerStatement,
            paramMap = mapOf(
                "omsorgspengerSaksnummer" to "$omsorgspengerSaksnummer"
            )
        )

        val søker = mutableListOf<BehandlingId>()
        val motpart = mutableListOf<BehandlingId>()

        sessionOf(dataSource).use { session ->
            session.run(query.map { row ->
                when (Involvering.valueOf(row.string("type"))) {
                    Involvering.SØKER -> søker.add(row.long("behandling_id"))
                    Involvering.MOTPART -> motpart.add(row.long("behandling_id"))
                }
            }.asList)
        }

        return mapOf(
            Involvering.SØKER to søker,
            Involvering.MOTPART to motpart
        )

    }

    internal fun hentOmsorgspengerSaksnummer(identitetsnummer: Identitetsnummer) : OmsorgspengerSaksnummer? {
        val query = queryOf(
            statement = HenteSaksnummerStatement,
            paramMap = mapOf(
                "identitetsnummer" to "$identitetsnummer"
            )
        )

        val saksnummer = sessionOf(dataSource).use { session ->
            session.run(query.map { row ->
                row.string("omsorgspenger_saksnummer")
            }.asList)
        }.toSet()

        require(saksnummer.size in 0..1) {
            SecureLogger.warn("Fant ${saksnummer.size} saksnummer for identitetsnummer $identitetsnummer: $saksnummer")
        }

        return saksnummer.firstOrNull()?.somOmsorgspengerSaksnumer()
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
                        "identitetsnummer" to part.identitetsnummer?.toString(),
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
                else -> throw IllegalStateException("[BehandlingId=$behandlingId] Støtter ikke å lagre part av type ${part.javaClass}")
            }}
            queries.forEach { query -> require(update(query) == 1) {
                "[BehandlingId=$behandlingId] Feil ved lagring av part"
            }}
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

        @Language("PostgreSQL")
        private const val HenteParterStatement = """
            SELECT * FROM part WHERE behandling_id = ANY(:behandlingIder)
        """

        @Language("PostgreSQL")
        private const val HenteInvolveringerStatement = """
            SELECT behandling_id, type FROM part 
            WHERE omsorgspenger_saksnummer = :omsorgspengerSaksnummer
        """

        @Language("PostgreSQL")
        private const val HenteSaksnummerStatement = """
            SELECT omsorgspenger_saksnummer FROM part 
            WHERE identitetsnummer = :identitetsnummer
        """
    }
}