package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.omsorgsdager.SerDes.map
import no.nav.omsorgsdager.SerDes.objectNode
import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt
import no.nav.omsorgsdager.behandlingId
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarnResponse
import no.nav.omsorgsdager.tilgangsstyring.Operasjon
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.kronisksyktbarn.dto.KronisktSyktBarnGrunnlag
import no.nav.omsorgsdager.kronisksyktbarn.dto.LøsteAksjonspunkterRequest
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.utÅretBarnetFyller18
import no.nav.omsorgsdager.vedtak.VedtakResponse
import no.nav.omsorgsdager.vedtak.VedtakStatus
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal fun Route.KroniskSyktBarnRoute(
    tilgangsstyring: Tilgangsstyring,
    kroniskSyktBarnRepository: KroniskSyktBarnRepository) {

    route("/kroniskt-sykt-barn") {

        post {
            val request = call.objectNode()
            val grunnlag = KronisktSyktBarnGrunnlag(request)

            tilgangsstyring.verifiserTilgang(call, Operasjoner.NyBehandlingKroniskSyktBarn.copy(
                identitetsnummer = setOf(
                    grunnlag.søker.identitetsnummer,
                    grunnlag.barn.identitetsnummer
                )
            ))

            val uløsteAksjonspunkter = setOf(UløstAksjonspunkt(navn = "LEGEERKLÆRING"))
            kroniskSyktBarnRepository.nyttVedtak(
                vedtak = KroniskSyktBarnVedtak(
                    saksnummer = grunnlag.saksnummer,
                    behandlingId = grunnlag.behandlingId,
                    status = VedtakStatus.FORSLAG,
                    statusSistEndret = ZonedDateTime.now(),
                    barn = grunnlag.barn,
                    periode = Periode(
                        fom = grunnlag.mottatt.toLocalDate(),
                        tom = grunnlag.barn.fødselsdato.utÅretBarnetFyller18()
                    )
                ),
                uløsteAksjonspunkter = uløsteAksjonspunkter
            )

            val response = VedtakResponse(
                status = VedtakStatus.FORSLAG,
                uløsteAksjonspunkter = uløsteAksjonspunkter
            )

            call.respond(HttpStatusCode.Created, response.toJson())
        }

        put("/{behandlingId}/aksjonspunkt") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORSLAG) {
                call.respond(HttpStatusCode.Conflict)
                return@put
            }

            val request = call.objectNode()
            val løsteAksjonspunkter = request.map<LøsteAksjonspunkterRequest>()

            // TODO: Hent behandling + identer

            tilgangsstyring.verifiserTilgang(call, Operasjoner.LøseAksjonspunktKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.løsteAksjonspunkter(
                behandlingId = behandlingId,
                løsteAksjonspunkter = løsteAksjonspunkter.løsteAksjonspunkter
            )

            val response = VedtakResponse(
                status = vedtak.status,
                uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter
            )

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        put("/{behandlingId}/fastsett") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORSLAG) {
                call.respond(HttpStatusCode.Conflict)
                return@put
            }
            if (vedtakOgAksjonspunkter.second.uløsteAksjonspunkter.isNotEmpty()) {
                call.respond(HttpStatusCode.Conflict)
                return@put
            }

            // TODO: Løst alt men ikke kan fastsettes...

            tilgangsstyring.verifiserTilgang(call, Operasjoner.FastsetteKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (fastsattVedtak, fastsattAksjonspunkter) = kroniskSyktBarnRepository.fastsett(
                behandlingId = behandlingId
            )

            val response = VedtakResponse(
                status = fastsattVedtak.status,
                uløsteAksjonspunkter = fastsattAksjonspunkter.uløsteAksjonspunkter
            )


            // TODO: Oppdater db & send till k9-vaktmester

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        put("/{behandlingId}/deaktiver") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORSLAG) {
                call.respond(HttpStatusCode.Conflict)
                return@put
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.DeaktivereKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.deaktiver(
                behandlingId = behandlingId
            )

            val response = VedtakResponse(
                status = vedtak.status,
                uløsteAksjonspunkter = aksjonspunkter.uløsteAksjonspunkter
            )

            call.respond(HttpStatusCode.OK, response.toJson())
        }

        get("/{behandlingId}") {

            val behandlingId = call.parameters.getOrFail("behandlingId")
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.HenteKroniskSyktBarnBehandling.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val behandling = HentKroniskSyktBarnResponse(
                vedtak = vedtakOgAksjonspunkter.first,
                aksjonspunkter = vedtakOgAksjonspunkter.second
            )

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