package no.nav.omsorgsdager.utvidetrett

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

internal fun Route.UtvidetRett() {

    route("/utvidet-rett") {
        post {
            val payload = try {
                call.receive<UtvidetRettRequestBody>()
            } catch (cause: Throwable) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            call.respond(HttpStatusCode.NotImplemented)
        }

        post("/aksjonspunkt") {
            val request = """
            "request": {
                "VURDERE_LEGEERKLÆRING": {
                    "foo": true,
                    "bar": false
                }
            }
            """.trimIndent()
            val response = """
            "response": {
                "id": 1,
                "status": "FORSLAG",
                "søker": {},
                "barn": {},
                "gyldigFraOgMed": "LocalDate",
                "gyldigTilOgMed": "LocalDate",
                "aksjonspunkter": []
            }
            """.trimIndent()
            call.respond(HttpStatusCode.NotImplemented)
        }

        put("/{id}/fastsett") {
            call.respond(HttpStatusCode.NotImplemented)
        }

        delete("/{id}") {
            val response = """
                "response": {
                    "id": 1,
                    "søker": {},
                    "barn": {},
                    "status": "AVSLÅTT",
                    "gyldigFraOgMed": "LocalDate",
                    "gyldigTilOgMed": "LocalDate",
                    "aksjonspunkter": []
                }
            """.trimIndent()
            call.respond(HttpStatusCode.NotImplemented)
        }

    }

}