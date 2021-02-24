package no.nav.omsorgsdager.behandling.db

import kotliquery.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.behandling.NyBehandling
import no.nav.omsorgsdager.tid.Periode
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class BehandlingRepository(
    private val dataSource: DataSource) {

    internal fun hentEn(behandlingId: K9BehandlingId) : DbBehandling? {
        val query = queryOf(
            statement = HentEnkeltbehandlingStatement,
            paramMap = mapOf(
                "behandlingId" to "$behandlingId"
            )
        )

        return sessionOf(dataSource).use { session ->
            session.run(query.map { row -> row.somDbBehandling() }.asSingle)
        }
    }

    internal fun hentAlle(saksnummer: K9Saksnummer) : List<DbBehandling> {
        throw NotImplementedError("Ikke implementert å hente basert på K9Saksnummer")
    }

    internal fun hentAlle(behandlingIder: List<BehandlingId>) : List<DbBehandling> {
        return sessionOf(dataSource).use { session ->
            val query = queryOf(
                statement = HentAlleBehandlingerStatement,
                paramMap = mapOf("behandlingIder" to session.createArrayOf("oid", behandlingIder))
            )
            session.run(query.map { row -> row.somDbBehandling() }.asList)
        }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(BehandlingRepository::class.java)

        internal fun TransactionalSession.lagreBehandling(behandling: NyBehandling) : BehandlingId? {
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

            return when (nyBehandlingId) {
                null -> {
                    val query = queryOf(
                        statement = HentBehandlingNøkkelinfoStatement,
                        paramMap = mapOf("behandlingId" to "${behandling.behandlingId}")
                    )

                    val (eksisterendeBehandlingId, eksisterendeGrunnlag, eksiterendeStatus) = run(query.map { row -> Triple(
                        row.long("id"),
                        row.string("grunnlag").somJson(),
                        BehandlingStatus.valueOf(row.string("status"))
                    )}.asSingle) ?: throw IllegalStateException("Fikk ikke lagret behandling, og finner ingen eksisterende med K9BehandlingId=[${behandling.behandlingId}]")

                    return when {
                        behandling.grunnlag != eksisterendeGrunnlag -> throw IllegalStateException(
                            "Behandling er allerede lagret, men med et annet grunnlag. K9BehandlingId=[${behandling.behandlingId}]")
                        behandling.status != eksiterendeStatus -> throw IllegalStateException(
                            "Behandlingen er allerede lagret, men med en annen status. K9BehandlingId=[${behandling.behandlingId}], EksisterendeStatus=[$eksiterendeStatus], NyStatus=[${behandling.status}]")
                        else -> logger.info("Allerede lagret med behandlingId=$eksisterendeBehandlingId").let { null }
                    }
                }
                else -> nyBehandlingId.also {
                    logger.info("Lagret med behandlingId=$it")
                }
            }
        }

        private fun Row.somDbBehandling() = DbBehandling(
            id = long("id"),
            k9Saksnummer = string("k9_saksnummer").somK9Saksnummer(),
            k9behandlingId = string("k9_behandling_id").somK9BehandlingId(),
            status = BehandlingStatus.valueOf(string("status")),
            tidspunkt = zonedDateTime("tidspunkt"),
            type = BehandlingType.valueOf(string("type")),
            periode = Periode(
                fom = localDate("fom"),
                tom = localDate("tom")
            )
        )

        @Language("PostgreSQL")
        private const val HentBehandlingNøkkelinfoStatement = """
            SELECT id, status, grunnlag from behandling where k9_behandling_id = :behandlingId
        """

        @Language("PostgreSQL")
        private const val HentAlleBehandlingerStatement = """
            SELECT * FROM behandling WHERE id = ANY(:behandlingIder)
        """

        @Language("PostgreSQL")
        private const val HentEnkeltbehandlingStatement = """
            SELECT * FROM behandling WHERE k9_behandling_id = :behandlingId
        """

        @Language("PostgreSQL")
        private const val LagreBehandlingStatement = """
            INSERT INTO behandling (k9_saksnummer, k9_behandling_id, status, type, tidspunkt, fom, tom, grunnlag)
            VALUES(:saksnummer, :behandlingId, :status, :type, :tidspunkt, :fom, :tom, :grunnlag ::jsonb)
            ON CONFLICT (k9_behandling_id, type) DO NOTHING 
        """
    }
}