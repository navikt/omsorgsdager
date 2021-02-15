package no.nav.omsorgsdager.behov

import kotliquery.Query
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.VedtakId
import no.nav.omsorgsdager.lovverk.Lovanvendelser
import org.intellij.lang.annotations.Language

internal object BehovRepository {
    internal fun TransactionalSession.leggTilBehov(vedtakId: VedtakId, behov: Behov) : Behov {
        leggTilUløstBehov(vedtakId, behov.uløsteBehov)
        leggTilLøsteBehov(vedtakId, behov.løsteBehov)
        return hentBehov(vedtakId)
    }

    private fun TransactionalSession.leggTilUløstBehov(vedtakId: VedtakId, uløsteBehov: Set<UløstBehov>) {
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

    internal fun TransactionalSession.leggTilLøsteBehov(vedtakId: VedtakId, løsteBehov: Set<LøstBehov>) {
        løsteBehov.forEach { løstBehov ->
            lagre(queryOf(
                statement = LeggTilLøsteBehovStatement,
                paramMap = mapOf(
                    "vedtakId" to vedtakId,
                    "versjon" to løstBehov.versjon,
                    "navn" to løstBehov.navn,
                    "losning" to løstBehov.løsning.raw,
                    "lovanvendelser" to løstBehov.lovanvendelser.somJson().raw
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

        run(query.map { row -> mapOf<String, Any?>(
            "status" to row.string("status"),
            "navn" to row.string("navn"),
            "versjon" to row.intOrNull("versjon"),
            "lovanvendelser" to row.stringOrNull("lovanvendelser"),
            "losning" to row.stringOrNull("losning")
        )}.asList).forEach { entry ->
            val status = entry["status"] as String
            when (status) {
                "LØST" -> løsteBehov.add(TidligereLøstBehov(
                    navn = entry["navn"] as String,
                    versjon = entry["versjon"] as Int,
                    lovanvendelser = (entry["lovanvendelser"] as String).somJson().let { Lovanvendelser.fraJson(it) },
                    løsning = (entry["losning"] as String).somJson()
                ))
                "ULØST" -> uløsteBehov.add(UløstBehov(
                    navn = entry["navn"] as String
                ))
                else -> throw IllegalStateException("Uventet status $status")
            }
        }
        return Behov(uløsteBehov = uløsteBehov, løsteBehov = løsteBehov)
    }

    private fun TransactionalSession.lagre(query: Query) {
        update(query).also { affectedRows ->
            require(affectedRows == 1) {
                "Oppdaterte $affectedRows rader, forventet å oppdatere 1."
            }
        }
    }

    /**
     * Skal kun være mulig å endre på behov gitt at vedtaket har
     * status = 'FORESLÅTT'.
     * Om det har en annen status får vi ingen vedtakId og det kommer ut som en feil
     * `org.postgresql.util.PSQLException: ERROR: null value in column "vedtak_id" violates not-null constraint`
     */
    @Language("PostgreSQL")
    private const val VedtakIdForVedtakForsikretIStatusForeslått = """
        (SELECT id from vedtak WHERE id = :vedtakId AND status = 'FORESLÅTT')
    """
    @Language("PostgreSQL")
    private const val LeggTilUløstBehovStatement = """
        INSERT into behov (vedtak_id, navn)
        VALUES ($VedtakIdForVedtakForsikretIStatusForeslått, :navn)
        ON CONFLICT DO NOTHING
    """

    @Language("PostgreSQL")
    private const val LeggTilLøsteBehovStatement = """
        UPDATE behov 
        SET status = 'LØST', versjon = :versjon, losning = :losning ::jsonb, lovanvendelser = :lovanvendelser ::jsonb
        WHERE vedtak_id = $VedtakIdForVedtakForsikretIStatusForeslått
        AND navn = :navn
    """

    @Language("PostgreSQL")
    private const val HentBehovStatement = """
        SELECT * from behov
        WHERE vedtak_id = :vedtakId
    """
}