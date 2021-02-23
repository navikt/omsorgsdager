package no.nav.omsorgsdager.behandling.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.NyBehandling
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class BehandlingRepository(
    private val dataSource: DataSource) {

    internal fun hentEn(behandlingId: K9BehandlingId) : DbBehandling? {
        return null
    }

    internal fun hentAlle(saksnummer: K9Saksnummer) : List<DbBehandling> {
        return emptyList()
    }

    internal fun hentAlle(behandlingIder: List<BehandlingId>) : List<DbBehandling> {
        return emptyList()
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
                        statement = HentBehandlingNøkkelinfo,
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

        @Language("PostgreSQL")
        private const val HentBehandlingNøkkelinfo = """
            SELECT id, status, grunnlag from behandling where k9_behandling_id = :behandlingId
        """


        @Language("PostgreSQL")
        private const val LagreBehandlingStatement = """
            INSERT INTO behandling (k9_saksnummer, k9_behandling_id, status, type, tidspunkt, fom, tom, grunnlag)
            VALUES(:saksnummer, :behandlingId, :status, :type, :tidspunkt, :fom, :tom, :grunnlag ::jsonb)
            ON CONFLICT (k9_behandling_id, type) DO NOTHING 
        """
    }
}