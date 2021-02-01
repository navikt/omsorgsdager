package no.nav.omsorgsdager.kronisksyktbarn

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.omsorgsdager.tilgangsstyring.Operasjon
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.kronisksyktbarn.dto.KronisktSyktBarnGrunnlag
import no.nav.omsorgsdager.vedtak.Aksjonspunkt
import no.nav.omsorgsdager.vedtak.VedtakResponse
import no.nav.omsorgsdager.vedtak.VedtakStatus
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

internal fun Route.KroniskSyktBarnRoute(
    tilgangsstyring: Tilgangsstyring,
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

            val grunnlag = KronisktSyktBarnGrunnlag(request as ObjectNode)

            tilgangsstyring.verifiserTilgang(call, Operasjoner.NyBehandlingKroniskSyktBarn.copy(
                identitetsnummer = setOf(
                    grunnlag.søker.identitetsnummer,
                    grunnlag.barn.identitetsnummer
                )
            ))

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

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandling + identer

            tilgangsstyring.verifiserTilgang(call, Operasjoner.LøseAksjonspunktKroniskSyktBarn.copy(
                identitetsnummer = setOf("123") // TODO
            ))

            // TODO: status=FASTSATT throws HttpStatusCode.Conflict
            val response = VedtakResponse(
                status = VedtakStatus.FORSLAG,
                uløsteAksjonspunkter = listOf(Aksjonspunkt(navn="MEDLEMSKAP"))
            )

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        put("/{behandlingId}/fastsett") {

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandlingId + identer + aksjonspunkter
            val aksjonspunkter = listOf(Aksjonspunkt("VURDERE_LEGEERKLÆRING"))

            tilgangsstyring.verifiserTilgang(call, Operasjoner.FastsetteKroniskSyktBarn.copy(
                identitetsnummer = setOf("123","456") // TODO
            ))

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

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandlingId + identer

            tilgangsstyring.verifiserTilgang(call, Operasjoner.DeaktivereKroniskSyktBarn.copy(
                identitetsnummer = setOf("123", "456") // TODO
            ))

            val response = VedtakResponse(
                status = VedtakStatus.DEAKTIVERT,
                uløsteAksjonspunkter = emptyList())


            // TODO: Oppdater db
            //utvidettRepository.fastsett(behandlingId)

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        get("/{behandlingId}") {

            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandling + identer

            tilgangsstyring.verifiserTilgang(call, Operasjoner.HenteKroniskSyktBarnBehandling.copy(
                identitetsnummer = setOf("123", "456")
            ))

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
            val behandlingId = call.parameters["behandlingId"]

            // TODO: Hent behandling + identer

            tilgangsstyring.verifiserTilgang(call, Operasjoner.HenteKroniskSyktBarnSak.copy(
                identitetsnummer = setOf("123", "456")
            ))

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

private object Operasjoner {
    val NyBehandlingKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Registrere ny behandling på søknad om kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val FastsetteKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Fastsette vedtak for kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val DeaktivereKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Deaktivere vedtak for kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val HenteKroniskSyktBarnBehandling = Operasjon(
        type = Operasjon.Type.Visning,
        beskrivelse = "Hente vedtak for kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val HenteKroniskSyktBarnSak = Operasjon(
        type = Operasjon.Type.Visning,
        beskrivelse = "Hente gjeldende vedtak for kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val LøseAksjonspunktKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Visning,
        beskrivelse = "Løse aksjonspunkt for kronisk sykt barn",
        identitetsnummer = setOf()
    )
}