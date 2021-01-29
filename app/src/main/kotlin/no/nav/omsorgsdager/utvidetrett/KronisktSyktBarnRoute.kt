package no.nav.omsorgsdager.utvidetrett

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.omsorgsdager.Operasjon
import no.nav.omsorgsdager.TilgangsstyringRestClient
import no.nav.omsorgsdager.utvidetrett.dto.AksjonspunktRequest
import no.nav.omsorgsdager.utvidetrett.dto.KronisktSyktBarnGrunnlag
import no.nav.omsorgsdager.vedtak.Aksjonspunkt
import no.nav.omsorgsdager.vedtak.VedtakResponse
import no.nav.omsorgsdager.vedtak.VedtakStatus
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal fun Route.KronisktSyktBarnRoute(
    tilgangsstyringRestClient: TilgangsstyringRestClient,
    kafkaProducer: KafkaProducer<String, String>,
    utvidettRepository: UtvidettRepository
) {

    val logger = LoggerFactory.getLogger("no.nav.omsorgsdager.KronisktSyktBarnRoute")

    route("/kroniskt-sykt-barn") {

        post {
            val request = try {
                call.receive<JsonNode>()
            } catch (cause: Throwable) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val identer = setOf(
                request["barn"]["identitetsnummer"].toString(),
                request["søker"]["identitetsnummer"].toString()
            )

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = identer,
                authHeader = authHeader,
                beskrivelse = "Registrere ny søknad før kroniskt sykt barn",
                operasjon = Operasjon.Endring
            )

            if (!harTilgang) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }

            val grunnlag = KronisktSyktBarnGrunnlag(request as ObjectNode)

            val response = VedtakResponse(
                status = VedtakStatus.FORSLAG,
                uløsteAksjonspunkter = listOf(Aksjonspunkt("VURDERE_LEGEERKLÆRING"))
            )

            //utvidettRepository.lagre(request.behandlingId(), request.toString())
            call.respond(HttpStatusCode.OK, response.toJson())
        }

        // Oppdatere i database
        put("/{behandlingId}/aksjonspunkt") {
            val request = try {
                call.receive<JsonNode>()
            } catch (cause: Throwable) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@put call.respond(HttpStatusCode.Unauthorized)

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandling + identer

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf("123"),
                authHeader = authHeader,
                beskrivelse = "Legge till eller oppdatere aksjonspunkt",
                operasjon = Operasjon.Endring
            )

            if (!harTilgang) {
                return@put call.respond(HttpStatusCode.Forbidden)
            }

            // TODO: status=FASTSATT throws HttpStatusCode.Conflict
            val response = VedtakResponse(
                status = VedtakStatus.FORSLAG,
                uløsteAksjonspunkter = listOf(Aksjonspunkt(navn="MEDLEMSKAP"))
            )

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        put("/{behandlingId}/fastsett") {

            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@put call.respond(HttpStatusCode.Unauthorized)

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandlingId + identer + aksjonspunkter
            val aksjonspunkter = listOf(Aksjonspunkt("VURDERE_LEGEERKLÆRING"))
            val identer = setOf("123", "456")

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = identer,
                authHeader = authHeader,
                beskrivelse = "Fastsette vedtak",
                operasjon = Operasjon.Endring
            )

            if (!harTilgang) {
                return@put call.respond(HttpStatusCode.Forbidden)
            }

            // se efter uløste Aksjonspunkter
            val uløsteAksjonspunkter = 0
            if(uløsteAksjonspunkter>0) {
                call.respond(HttpStatusCode.Conflict, uløsteAksjonspunkter)
            }

            val response = VedtakResponse(
                status = VedtakStatus.FASTSATT,
                uløsteAksjonspunkter = emptyList())


            // TODO: Oppdater db
            //utvidettRepository.fastsett(behandlingId)

            // TODO: Send till k9-vaktmester

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        put("/{behandlingId}/deaktiver") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@put call.respond(HttpStatusCode.Unauthorized)

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandlingId + identer
            val identer = setOf("123", "456")

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = identer,
                authHeader = authHeader,
                beskrivelse = "Fastsette vedtak",
                operasjon = Operasjon.Endring
            )

            if (!harTilgang) {
                return@put call.respond(HttpStatusCode.Forbidden)
            }

            val response = VedtakResponse(
                status = VedtakStatus.DEAKTIVERT,
                uløsteAksjonspunkter = emptyList())


            // TODO: Oppdater db
            //utvidettRepository.fastsett(behandlingId)

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        get("/{behandlingId}") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandling + identer
            val identer = setOf("123", "456")

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = identer,
                authHeader = authHeader,
                beskrivelse = "Hente ett vedtak",
                operasjon = Operasjon.Visning
            )

            if (!harTilgang) {
                return@get call.respond(HttpStatusCode.Forbidden)
            }

            val behandling = """
                {
                      "barn": {
                        "identitetsnummer": "$behandlingId",
                        "fødselsdato": "2021-01-29"
                      },
                      "behandlingId": "UUID-123-123",
                      "gyldigFraOgMed": "2021-01-29",
                      "gyldigTilOgMed": "2021-01-29",
                      "status": "FORSLAG",
                      "uløsteAksjonspunkter": {
                        "LEGEERKLÆRING": {}
                      },
                      "løsteAksjonspunkter": {
                        "MEDLEMSKAP": {},
                        "YRKESAKTIVITET": {}
                      },
                      "lovhenvisnigner": {
                        "FTL 9-5 3.ledd": "søkeren bor ikke i norge",
                        "FTL 9-5 2.ledd": "ikke omsorgen for barnet"
                      }
                }
            """.trimIndent()

            call.respond(HttpStatusCode.OK, behandling)
        }

        get("{saksnummer}") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandling + identer
            val identer = setOf("123", "456")

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = identer,
                authHeader = authHeader,
                beskrivelse = "Hente ett vedtak",
                operasjon = Operasjon.Visning
            )

            if (!harTilgang) {
                return@get call.respond(HttpStatusCode.Forbidden)
            }

            val behandling = """
                {
                      "barn": {
                        "identitetsnummer": "$behandlingId",
                        "fødselsdato": "2021-01-29"
                      },
                      "behandlingId": "UUID-123-123",
                      "gyldigFraOgMed": "2021-01-29",
                      "gyldigTilOgMed": "2021-01-29",
                      "status": "FORSLAG",
                      "uløsteAksjonspunkter": {
                        "LEGEERKLÆRING": {}
                      },
                      "løsteAksjonspunkter": {
                        "MEDLEMSKAP": {},
                        "YRKESAKTIVITET": {}
                      },
                      "lovhenvisnigner": {
                        "FTL 9-5 3.ledd": "søkeren bor ikke i norge",
                        "FTL 9-5 2.ledd": "ikke omsorgen for barnet"
                      }
                }
            """.trimIndent()

            call.respond(HttpStatusCode.OK, behandling)
        }
    }

}