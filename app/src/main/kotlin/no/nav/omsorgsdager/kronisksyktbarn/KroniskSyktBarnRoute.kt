package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.SerDes.map
import no.nav.omsorgsdager.SerDes.objectNode
import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt
import no.nav.omsorgsdager.behandlingId
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSkytBarnListeResponse
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarnRequest.Companion.hentKroniskSyktBarnRequest
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarnResponse
import no.nav.omsorgsdager.tilgangsstyring.Operasjon
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.kronisksyktbarn.dto.KronisktSyktBarnGrunnlag
import no.nav.omsorgsdager.kronisksyktbarn.dto.LøsteAksjonspunkterRequest
import no.nav.omsorgsdager.saksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.gjeldendeVedtak
import no.nav.omsorgsdager.vedtak.VedtakResponse
import no.nav.omsorgsdager.vedtak.VedtakStatus
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
                        fom = grunnlag.mottatt.toLocalDateOslo(),
                        tom = grunnlag.barn.fødselsdato.sisteDagIÅretOm18År()
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

        suspend fun ApplicationCall.hentForBehandlingId(behandlingId: BehandlingId) : HentKroniskSyktBarnResponse? {
            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)?:return null

            tilgangsstyring.verifiserTilgang(this, Operasjoner.HenteKroniskSyktBarnBehandling.copy(
                identitetsnummer = vedtak.involverteIdentitetsnummer
            ))

            return HentKroniskSyktBarnResponse(
                vedtak = vedtak,
                aksjonspunkter = aksjonspunkter
            )
        }

        suspend fun ApplicationCall.hentForSaksnummer(saksnummer: Saksnummer) : HentKroniskSkytBarnListeResponse {
            val alle = kroniskSyktBarnRepository.hentAlle(saksnummer = saksnummer)
            val gjeldendeVedtak = alle.map { it.first }.gjeldendeVedtak()
            val identitetsnummer = gjeldendeVedtak.map { it.involverteIdentitetsnummer }.flatten().toSet()

            tilgangsstyring.verifiserTilgang(this, Operasjoner.HenteKroniskSyktBarnSak.copy(
                identitetsnummer = identitetsnummer
            ))

            val vedtak =  gjeldendeVedtak.map { gjeldendeVedtak -> HentKroniskSyktBarnResponse(
                vedtak = gjeldendeVedtak,
                aksjonspunkter = alle.first { it.first.behandlingId == gjeldendeVedtak.behandlingId }.second
            )}
            return HentKroniskSkytBarnListeResponse(vedtak)
        }

        get {
            val request = call.hentKroniskSyktBarnRequest()
            when (request.hentForBehandlingId) {
                true -> {
                    val response = call.hentForBehandlingId(
                        behandlingId = request.behandlingId!!
                    )
                    when (response) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respond(HttpStatusCode.OK, response)
                    }
                }
                false -> {
                    call.respond(HttpStatusCode.OK, call.hentForSaksnummer(
                        saksnummer = request.saksnummer!!
                    ))
                }
            }
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