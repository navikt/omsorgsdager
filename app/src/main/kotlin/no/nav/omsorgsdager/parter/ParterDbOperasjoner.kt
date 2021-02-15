package no.nav.omsorgsdager.parter

import kotliquery.Query
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.VedtakId
import no.nav.omsorgsdager.vedtak.VedtakDbOperasjoner.VedtakIdForVedtakForsikretIStatusForeslått
import org.intellij.lang.annotations.Language
import java.time.LocalDate

internal object ParterDbOperasjoner {
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

        return run(query.map { row ->
            val type = row.string("type")
            when (type) {
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
                    identitetsnummer = row.stringOrNull("identitetsnummer")
                )
                else -> throw IllegalStateException("Ukjent type $type på part")
            }
        }.asList)
    }

    internal fun TransactionalSession.leggTilParter(vedtakId: VedtakId, parter: List<Part>) : List<Part> {
        parter.forEach { part ->
            val paramMap = when (part) {
                is Søker -> mapOf(
                    "vedtakId" to vedtakId,
                    "identitetsnummer" to part.identitetsnummer,
                    "fodelsdato" to null,
                    "omsorgspengerSaksnummer" to part.omsorgspengerSaksnummer,
                    "type" to "SØKER"
                )
                is Barn -> mapOf(
                    "vedtakId" to vedtakId,
                    "identitetsnummer" to part.identitetsnummer,
                    "fodelsdato" to part.fødselsdato,
                    "type" to "BARN"
                )
                is Motpart -> mapOf(
                    "vedtakId" to vedtakId,
                    "identitetsnummer" to part.identitetsnummer,
                    "fodelsdato" to null,
                    "omsorgspengerSaksnummer" to part.omsorgspengerSaksnummer,
                    "type" to "MOTPART"
                )
                else -> throw IllegalStateException("Uhåndtert part $part")
            }
            lagre(queryOf(
                statement = LeggTilPartStatement,
                paramMap = paramMap
            ))
        }
        return hentParter(vedtakId)
    }

    private fun TransactionalSession.lagre(query: Query) {
        update(query).also { affectedRows ->
            require(affectedRows == 1) {
                "Oppdaterte $affectedRows rader, forventet å oppdatere 1."
            }
        }
    }

    @Language("PostgreSQL")
    private const val LeggTilPartStatement = """
        INSERT INTO parter (vedtak_id, identitetsnummer, fodselsdato, type, omsorgspenger_saksnummer)
        VALUES($VedtakIdForVedtakForsikretIStatusForeslått, :identitetsnummer, :fodelsdato, :type, :omsorgspengerSaksnummer)
    """

    @Language("PostgreSQL")
    private const val HentParterStatement = """
        SELECT * from parter
        WHERE vedtak_id = :vedtakId
    """
}