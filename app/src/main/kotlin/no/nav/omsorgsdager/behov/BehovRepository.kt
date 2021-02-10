package no.nav.omsorgsdager.behov

import kotliquery.Query
import kotliquery.Session
import kotliquery.queryOf
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.VedtakId
import no.nav.omsorgsdager.lovverk.Lovanvendelser
import org.intellij.lang.annotations.Language

internal object BehovRepository {
    internal fun Session.leggTilUløstBehov(vedtakId: VedtakId, uløsteBehov: Set<UløstBehov>) {
        uløsteBehov.forEach { uløstBehov ->
            update(queryOf(
                statement = LeggTilUløstBehovStatement,
                paramMap = mapOf(
                    "vedtakId" to vedtakId,
                    "navn" to uløstBehov.navn
                )
            ))
        }
    }

    internal fun Session.leggTilLøsteBehov(vedtakId: VedtakId, løsteBehov: Set<LøstBehov>) {
        løsteBehov.forEach { løstBehov ->
            lagre(queryOf(
                statement = LeggTilLøsteBehovStatement,
                paramMap = mapOf(
                    "vedtakId" to vedtakId,
                    "versjon" to løstBehov.versjon,
                    "navn" to løstBehov.navn,
                    "losning" to løstBehov.løsning.raw
                )
            ))
        }
    }

    internal fun Session.hentBehov(vedtakId: VedtakId) : Behov {
        val query = queryOf(
            statement = HentBehovStatement,
            paramMap = mapOf(
                "vedtakId" to vedtakId
            )
        )

        val uløsteBehov = mutableSetOf<UløstBehov>()
        val løsteBehov = mutableSetOf<LøstBehov>()
        run(query.map{ it }.asList).forEach { row ->
            val status = row.string("status")
            when (status) {
                "ULØST" -> uløsteBehov.add(UløstBehov(navn = row.string("navn")))
                "LØST" -> løsteBehov.add(TidligereLøstBehov(
                    navn = row.string("navn"),
                    versjon = row.int("versjon"),
                    lovanvendelser = Lovanvendelser(row.string("lovanvendelser").somJson()),
                    løsning = row.string("losning").somJson()
                ))
                else -> throw IllegalStateException("Uventet status på behov $status")
            }
        }
        return Behov(uløsteBehov = uløsteBehov, løsteBehov = løsteBehov)
    }

    private fun Session.lagre(query: Query) {
        update(query).also { affectedRows ->
            require(affectedRows == 1) {
                "Oppdaterte $affectedRows rader, forventet å oppdatere 1."
            }
        }
    }

    @Language("PostgreSQL")
    private const val LeggTilUløstBehovStatement = """
        INSERT into behov (vedtak_id, navn)
        VALUES (:vedtakId, :navn)
        ON CONFLICT DO NOTHING
    """

    @Language("PostgreSQL")
    private const val LeggTilLøsteBehovStatement = """
        UPDATE behov 
        SET status = 'LØST', versjon = :versjon, losning = :losning ::jsonb, lovanvendeler = :lovanvendelser ::jsonb
        WHERE vedtak_id = :vedtakId
        AND navn = :navn
    """

    @Language("PostgreSQL")
    private const val HentBehovStatement = """
        SELECT * from behov
        WHERE vedtak_id = :vedtakId
    """
}