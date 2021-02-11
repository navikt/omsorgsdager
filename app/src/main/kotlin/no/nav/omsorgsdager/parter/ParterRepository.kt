package no.nav.omsorgsdager.parter

import kotliquery.Query
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.VedtakId
import org.intellij.lang.annotations.Language
import java.time.LocalDate

internal object ParterRepository {
    interface Part

    internal data class Søker(
        internal val identitetsnummer: Identitetsnummer,
        internal val omsorgspengerSaksnummer: Saksnummer) : Part

    internal data class Barn(
        internal val fødselsdato: LocalDate,
        internal val identitetsnummer: Identitetsnummer?) : Part

    internal data class Motpart(
        internal val identitetsnummer: Identitetsnummer,
        internal val omsorgspengerSaksnummer: Saksnummer) : Part

    internal fun Session.hentParter(vedtakId: VedtakId) : List<Part> {
        val query = queryOf(
            statement = HentParterStatement,
            paramMap = mapOf(
                "vedtakId" to vedtakId
            )
        )

        val parter = mutableListOf<Part>()
        run(query.map{ it }.asList).forEach { row ->
            val type = row.string("type")
            val part = when (type) {
                "SØKER" -> Søker(
                    identitetsnummer = row.string("identitetsnummer"),
                    omsorgspengerSaksnummer = row.string("omsorgspenger_saksnummer")
                )
                "MOTPART" -> Motpart(
                    identitetsnummer = row.string("identitetsnummer"),
                    omsorgspengerSaksnummer = row.string("omsorgspenger_saksnummer")
                )
                "BARN" -> Barn(
                    fødselsdato = row.localDate("fodselsdato"),
                    identitetsnummer = row.string("identitetsnummer")
                )
                else -> throw IllegalStateException("Ukjent type $type på part")
            }
            parter.add(part)
        }
        return parter
    }

    internal fun TransactionalSession.leggTilParter(vedtakId: VedtakId, parter: List<Part>) {
        parter.forEach { part ->
            val paramMap = when (part) {
                is Søker -> mapOf(
                    "vedtakId" to vedtakId,
                    "identitetsnummer" to part.identitetsnummer,
                    "fodelsdato" to null,
                    "omsorgspengerSaksnummer" to part.identitetsnummer,
                    "part" to "SØKER"
                )
                is Barn -> mapOf(
                    "vedtakId" to vedtakId,
                    "identitetsnummer" to part.identitetsnummer,
                    "fodelsdato" to part.fødselsdato,
                    "part" to "BARN"
                )
                is Motpart -> mapOf(
                    "vedtakId" to vedtakId,
                    "identitetsnummer" to part.identitetsnummer,
                    "fodelsdato" to null,
                    "omsorgspengerSaksnummer" to part.identitetsnummer,
                    "part" to "MOTPART"
                )
                else -> throw IllegalStateException("Uhåndtert part $part")
            }
            lagre(queryOf(
                statement = LeggTilPartStatement,
                paramMap = paramMap
            ))
        }
    }

    private fun Session.lagre(query: Query) {
        update(query).also { affectedRows ->
            require(affectedRows == 1) {
                "Oppdaterte $affectedRows rader, forventet å oppdatere 1."
            }
        }
    }

    @Language("PostgreSQL")
    private const val LeggTilPartStatement = """
        INSERT INTO parter (vedtak_id, identitetsnummer, fodselsdato, type, omsorgsdager_saksnummer)
        VALUES(:vedtakId, :identitetsnummer, :fodelsdato, :type, :omsorgspengerSaksnummer)
    """

    @Language("PostgreSQL")
    private const val HentParterStatement = """
        SELECT * from part 
        WHERE vedtak_id = :vedtakId
    """
}