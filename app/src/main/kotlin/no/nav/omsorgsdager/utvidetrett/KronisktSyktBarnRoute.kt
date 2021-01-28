package no.nav.omsorgsdager.utvidetrett

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.omsorgsdager.Operasjon
import no.nav.omsorgsdager.TilgangsstyringRestClient
import no.nav.omsorgsdager.utvidetrett.dto.AksjonspunktRequest
import no.nav.omsorgsdager.utvidetrett.dto.KronisktSyktBarnSoknadRequest
import org.apache.kafka.clients.producer.KafkaProducer

internal fun Route.KronisktSyktBarn(
    tilgangsstyringRestClient: TilgangsstyringRestClient,
    kafkaProducer: KafkaProducer<String, String>
) {

    route("/kroniskt-sykt-barn") {

        // Lagra i database
        post {
            val payload = try {
                call.receive<KronisktSyktBarnSoknadRequest>()
            } catch (cause: Throwable) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf(payload.barn.identitetsnummer, payload.søker.identitetsnummer),
                authHeader = authHeader,
                beskrivelse = "Registrere ny søknad før kroniskt sykt barn",
                operasjon = Operasjon.Endring)

            if (!harTilgang) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }

            val aksjonspunkter = """
                "VURDERE_LEGEERKLÆRING": {},
                "MEDLEMSKAP": {},
                "YRKESAKTIVITET": {}
            """.trimIndent()

            val response = """
                {
                    "status": "FORSLAG",
                    "uløsteAksjonspunkter": {
                        $aksjonspunkter
                    }
                }
            """.trimIndent()


            call.respond(HttpStatusCode.OK, response)
        }

        // Oppdatere i database
        put("/{behandlingId}/aksjonspunkt") {
            val payload = try {
                call.receive<AksjonspunktRequest>()
            } catch (cause: Throwable) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@put call.respond(HttpStatusCode.Unauthorized)

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf("123"),
                authHeader = authHeader,
                beskrivelse = "Legge till eller oppdatere aksjonspunkt",
                operasjon = Operasjon.Endring)

            if (!harTilgang) {
                return@put call.respond(HttpStatusCode.Forbidden)
            }

            val behandlingId = call.parameters["behandlingId"]

            call.respond(HttpStatusCode.NotImplemented)
        }

        // Registrere k9-vaktmester
        // Oppdatere i database
        put("/{behandlingId}/fastsett") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        // Oppdatere i database
        put("/{behandlingId}/deaktiver") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        get("{behandlingId}") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        get("{saksnummer}") {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }

}