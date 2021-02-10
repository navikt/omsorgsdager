package no.nav.omsorgsdager.vedtak

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.VedtakId
import no.nav.omsorgsdager.tid.Periode
import org.intellij.lang.annotations.Language
import java.time.ZonedDateTime

internal object VedtakRepository {
    internal enum class VedtakType {
        KRONISK_SYKT_BARN,
        MIDLERTIDIG_ALENE
    }

    internal data class DbVedtak(
        internal val vedtakId: VedtakId,
        internal val saksnummer: Saksnummer,
        internal val behandlingId: BehandlingId,
        internal val grunnlag: Json,
        internal val type: VedtakType,
        internal val status: VedtakStatus,
        internal val statusSistEndret: ZonedDateTime,
        internal val periode: Periode?
    )

    internal data class LagreVedtak (
        internal val saksnummer: Saksnummer,
        internal val behandlingId: BehandlingId,
        internal val grunnlag: Json,
        internal val type: VedtakType
    )

    internal data class EndreStatus(
        internal val behandlingId: BehandlingId,
        internal val status: VedtakStatus,
        internal val tidspunkt: ZonedDateTime
    )

    internal data class EndrePeriode(
        internal val behandlingId: BehandlingId,
        internal val periode: Periode
    )

    internal fun Session.lagreVedtak(lagreVedtak: LagreVedtak) : DbVedtak = lagreOgHent(
        behandlingId = lagreVedtak.behandlingId,
        query = queryOf(
            statement = LagreVedtakStatement,
            paramMap = mapOf(
                "saksnummer" to lagreVedtak.saksnummer,
                "behandlingId" to lagreVedtak.behandlingId,
                "grunnlag" to lagreVedtak.grunnlag.raw,
                "type" to lagreVedtak.type.name
            )
        )
    )

    internal fun Session.endreVedtakStatus(endreStatus: EndreStatus) : DbVedtak = lagreOgHent(
        behandlingId = endreStatus.behandlingId,
        query = queryOf(
            statement = EndreStatusStatement,
            paramMap = mapOf(
                "status" to endreStatus.status.name,
                "behandlingId" to endreStatus.behandlingId,
                "statusSistEndret" to endreStatus.tidspunkt
            )
        )
    )

    internal fun Session.endreVedtakPeriode(endrePeriode: EndrePeriode) : DbVedtak = lagreOgHent(
        behandlingId = endrePeriode.behandlingId,
        query = queryOf(
            statement = EndrePeriodeStatement,
            paramMap = mapOf(
                "behandlingId" to endrePeriode.behandlingId,
                "fom" to endrePeriode.periode.fom,
                "tom" to endrePeriode.periode.tom
            )
        )
    )

    internal fun Session.hentVedtak(behandlingId: BehandlingId) : DbVedtak? {
        val query = queryOf(
            statement = HentStatement,
            paramMap = mapOf(
                "behandlingId" to behandlingId
            )
        )

        return run(query.map { row ->
            row.somDbVedtak()
        }.asSingle)
    }

    internal fun Session.hentAlleVedtak(saksnummer: Saksnummer) : List<DbVedtak> {
        val query = queryOf(
            statement = HentAlleStatement,
            paramMap = mapOf(
                "saksnummer" to saksnummer
            )
        )

        return run(query.map { row ->
            row.somDbVedtak()
        }.asList)
    }

    private fun Row.somDbVedtak() : DbVedtak {
        val fom = localDateOrNull("fom")
        val tom = localDateOrNull("tom")
        val periode = when (listOfNotNull(fom,tom).size) {
            2 -> Periode(fom = fom!!, tom = tom!!)
            0 -> null
            else -> throw IllegalStateException("Ugyldig periode fom=$fom, tom=$tom")
        }

        return DbVedtak(
            vedtakId = long("id"),
            saksnummer = string("k9_saksnummer"),
            behandlingId = string("k9_behandling_id"),
            periode = periode,
            grunnlag = string("grunnlag").somJson(),
            type = VedtakType.valueOf(string("type")),
            status = VedtakStatus.valueOf("status"),
            statusSistEndret = zonedDateTime("status_sist_endret")
        )
    }

    private fun Session.lagreOgHent(behandlingId: BehandlingId, query: Query) : DbVedtak {
        update(query).also { affectedRows ->
            require(affectedRows == 1) {
                "Oppdaterte $affectedRows rader, forventet å oppdatere 1."
            }
        }
        return requireNotNull(hentVedtak(behandlingId)) {
            "Fant ingen vedtak med behandlingId etter oppdatering."
        }
    }

    @Language("PostgreSQL")
    private const val LagreVedtakStatement = """
        INSERT INTO vedtak (k9_saksnummer, k9_behanlding_id, grunnlag, type, status)
        VALUES(:saksnummer, :behandlingId, :grunnlag ::jsonb, :type, 'FORESLÅTT')
        ON CONFLICT (k9_behanlding_id) 
        DO 
           UPDATE SET grunnlag = :grunnlag :: jsonb 
           WHERE status = 'FORESLÅTT'
    """

    @Language("PostgreSQL")
    private const val EndreStatusStatement = """
        UPDATE vedtak 
        SET status = :status, status_sist_endret = :statusSistEndret
        WHERE k9_behandling_id = :behandlingId
        AND status = 'FORESLÅTT'
    """

    @Language("PostgreSQL")
    private const val EndrePeriodeStatement = """
        UPDATE vedtak 
        SET fom = :fom, tom = :tom
        WHERE k9_behandling_id = :behandlingId
        AND status = 'FORESLÅTT'
    """

    @Language("PostgreSQL")
    private const val HentStatement = """
        SELECT * FROM vedtak 
        WHERE k9_behandling_id = :behandlingId
    """

    @Language("PostgreSQL")
    private const val HentAlleStatement = """
        SELECT * FROM vedtak 
        WHERE k9_saksnummer = :saksnummer
    """
}