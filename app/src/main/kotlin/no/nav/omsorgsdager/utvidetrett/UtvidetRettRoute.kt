package no.nav.omsorgsdager.utvidetrett

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.omsorgsdager.Operasjon
import no.nav.omsorgsdager.TilgangsstyringRestClient
import java.time.LocalDateTime

internal fun Route.UtvidetRett(
    tilgangsstyringRestClient: TilgangsstyringRestClient
) {

    route("/utvidet-rett") {

        post {
            val payload = try {
                call.receive<UtvidetRettRequestBody>()
            } catch (cause: Throwable) {
                println(cause)
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val authHeader = call.request.headers[HttpHeaders.Authorization]
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val harTilgang = tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf(payload.barn.identitetsnummer, payload.søker.identitetsnummer),
                authHeader = authHeader,
                beskrivelse = "utvidet-rett",
                operasjon = Operasjon.Endring)

            if (!harTilgang) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }

            val response = """
                {
                    "id": "1",
                    "søker": {
                        "identitetsnummer": "${payload.søker.identitetsnummer}"
                    },
                    "barn": { 
                        "identitetsnummer": "${payload.barn.identitetsnummer}"
                    },
                    "status": "FORSLAG",
                    "gyldigFraOgMed": "${LocalDateTime.now()}",
                    "gyldigTilOgMed": "${LocalDateTime.now().plusDays(10)}",
                    "aksjonspunkter": [
                        "VURDERE_LEGEERKLÆRING"
                    ]
                }
            """.trimIndent()


            call.respond(HttpStatusCode.OK, response)
        }

        post("/aksjonspunkt") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        put("/{id}/fastsett") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        delete("/{id}") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        get("{id}") {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }

}