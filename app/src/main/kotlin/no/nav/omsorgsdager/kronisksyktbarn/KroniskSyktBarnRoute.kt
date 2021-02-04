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
import no.nav.omsorgsdager.aksjonspunkt.kanInnvilges
import no.nav.omsorgsdager.behandlingId
import no.nav.omsorgsdager.kronisksyktbarn.dto.*
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarn
import no.nav.omsorgsdager.kronisksyktbarn.dto.HentKroniskSyktBarn.Request.Companion.hentKroniskSyktBarnRequest
import no.nav.omsorgsdager.tilgangsstyring.Operasjon
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.sisteDagIÅretOm18År
import no.nav.omsorgsdager.tid.Periode.Companion.toLocalDateOslo
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.erInnenforDatoer
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.filtrerPåDatoer
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.gjeldendeVedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import no.nav.omsorgsdager.vedtak.dto.VedtakNøkkelinformasjon
import java.time.ZonedDateTime

internal fun Route.KroniskSyktBarnRoute(
    tilgangsstyring: Tilgangsstyring,
    kroniskSyktBarnRepository: KroniskSyktBarnRepository) {

    route("/kroniskt-sykt-barn") {

        post {
            val request = call.objectNode()
            val grunnlag = OpprettKroniskSyktBarn.Grunnlag(request)

            tilgangsstyring.verifiserTilgang(call, Operasjoner.NyBehandlingKroniskSyktBarn.copy(
                identitetsnummer = when(grunnlag.barn.identitetsnummer) {
                    null -> setOf(grunnlag.søker.identitetsnummer, grunnlag.barn.identitetsnummer)
                    else -> setOf(grunnlag.søker.identitetsnummer)
                } as Set<String>
            ))

            val ekisterendeBehandling = kroniskSyktBarnRepository.hent(behandlingId = grunnlag.behandlingId)
            if(ekisterendeBehandling != null) {
                call.respond(HttpStatusCode.Conflict)
                return@post
            }

            val tilOgMed = minOf(
                grunnlag.barn.fødselsdato.sisteDagIÅretOm18År(),
                grunnlag.søker.sisteDagSøkerHarRettTilOmsorgsdager
            )

            val uløsteAksjonspunkter = setOf(UløstAksjonspunkt(navn = "LEGEERKLÆRING"))
            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.nyttVedtak(
                vedtak = KroniskSyktBarnVedtak(
                    saksnummer = grunnlag.saksnummer,
                    behandlingId = grunnlag.behandlingId,
                    status = VedtakStatus.FORESLÅTT,
                    statusSistEndret = ZonedDateTime.now(),
                    søker = grunnlag.søker,
                    barn = grunnlag.barn,
                    periode = Periode(
                        fom = grunnlag.mottatt.toLocalDateOslo(),
                        tom = tilOgMed
                    )
                ),
                uløsteAksjonspunkter = uløsteAksjonspunkter
            )

            call.respond(HttpStatusCode.Created, VedtakNøkkelinformasjon.Response(
                vedtak = vedtak,
                aksjonspunkter = aksjonspunkter
            ))
        }

        patch("/{behandlingId}/aksjonspunkt") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORESLÅTT) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }

            val request = call.objectNode()
            val løsteAksjonspunkter = request.map<LøsKroniskSyktBarnAksjonspunkt.Request>()

            // TODO: Hent behandling + identer

            tilgangsstyring.verifiserTilgang(call, Operasjoner.LøseAksjonspunktKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.løsteAksjonspunkter(
                behandlingId = behandlingId,
                løsteAksjonspunkter = løsteAksjonspunkter.løsteAksjonspunkter
            )

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak = vedtak,
                aksjonspunkter = aksjonspunkter
            ))
        }

        patch("/{behandlingId}/innvilgelse") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORESLÅTT) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }
            if (vedtakOgAksjonspunkter.second.uløsteAksjonspunkter.isNotEmpty()) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }
            if (!vedtakOgAksjonspunkter.second.løsteAksjonspunkter.kanInnvilges()) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.InnvilgeKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (innvilgetVedtak, innvilgetAksjonspunkter) = kroniskSyktBarnRepository.innvilg(
                behandlingId = behandlingId
            )

            // TODO: Oppdater db & send till k9-vaktmester

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak = innvilgetVedtak,
                aksjonspunkter = innvilgetAksjonspunkter
            ))
        }

        patch("/{behandlingId}/forkast") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORESLÅTT) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.ForkastKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.forkast(
                behandlingId = behandlingId
            )

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak = vedtak,
                aksjonspunkter = aksjonspunkter
            ))
        }

        patch("/{behandlingId}/avslag") {
            val behandlingId = call.behandlingId()
            val vedtakOgAksjonspunkter = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgAksjonspunkter == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }
            if (vedtakOgAksjonspunkter.first.status != VedtakStatus.FORESLÅTT) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.AvslåKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgAksjonspunkter.first.involverteIdentitetsnummer
            ))

            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.avslå(
                behandlingId = behandlingId
            )

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak = vedtak,
                aksjonspunkter = aksjonspunkter
            ))
        }

        suspend fun ApplicationCall.hentForBehandling(
            request: HentKroniskSyktBarn.Request) : HentKroniskSyktBarn.Response {
            val (vedtak, aksjonspunkter) = kroniskSyktBarnRepository.hent(behandlingId = request.behandlingId!!) ?: return HentKroniskSyktBarn.Response()

            if (!vedtak.erInnenforDatoer(fom = request.gyldigFraOgMed, tom = request.gyldigTilOgMed)) {
                return HentKroniskSyktBarn.Response()
            }

            tilgangsstyring.verifiserTilgang(this, Operasjoner.HenteKroniskSyktBarnBehandling.copy(
                identitetsnummer = vedtak.involverteIdentitetsnummer
            ))

            return HentKroniskSyktBarn.Response(
                vedtak = listOf(HentKroniskSyktBarn.Vedtak(
                    vedtak = vedtak,
                    aksjonspunkter = aksjonspunkter
                ))
            )
        }

        suspend fun ApplicationCall.hentForSak(
            request: HentKroniskSyktBarn.Request) : HentKroniskSyktBarn.Response {
            val alle = kroniskSyktBarnRepository.hentAlle(saksnummer = request.saksnummer!!)
            val gjeldendeVedtak = alle.map { it.first }.gjeldendeVedtak()
            val identitetsnummer = gjeldendeVedtak.map { it.involverteIdentitetsnummer }.flatten().toSet()

            tilgangsstyring.verifiserTilgang(this, Operasjoner.HenteKroniskSyktBarnSak.copy(
                identitetsnummer = identitetsnummer
            ))

            val vedtak = gjeldendeVedtak.filtrerPåDatoer(
                fom = request.gyldigFraOgMed,
                tom = request.gyldigTilOgMed
            ).map { gv -> HentKroniskSyktBarn.Vedtak(
                vedtak = gv,
                aksjonspunkter = alle.first { it.first.behandlingId == gv.behandlingId }.second
            )}

            return HentKroniskSyktBarn.Response(vedtak)
        }

        get {
            val request = call.hentKroniskSyktBarnRequest()
            when (request.hentForBehandling) {
                true -> call.respond(HttpStatusCode.OK, call.hentForBehandling(request))
                false -> call.respond(HttpStatusCode.OK, call.hentForSak(request))
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

    val InnvilgeKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Innvilge vedtak for kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val AvslåKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Avslå vedtak for kronisk sykt barn",
        identitetsnummer = setOf()
    )

    val ForkastKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Forkaste vedtak for kronisk sykt barn",
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