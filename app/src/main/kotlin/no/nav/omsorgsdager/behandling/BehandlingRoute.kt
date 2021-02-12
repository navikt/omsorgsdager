package no.nav.omsorgsdager.behandling

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json.Companion.json
import no.nav.omsorgsdager.behandlingId
import no.nav.omsorgsdager.correlationId
import no.nav.omsorgsdager.tilgangsstyring.Operasjon
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.erInnenforDatoer
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.filtrerPåDatoer
import no.nav.omsorgsdager.vedtak.Vedtak.Companion.gjeldendeVedtak
import no.nav.omsorgsdager.behandling.dto.EndreVedtakStatus.endreVedtakStatusTidspunkt
import no.nav.omsorgsdager.behandling.dto.HentBehandling
import no.nav.omsorgsdager.behandling.dto.HentBehandling.Request.Companion.hentBehandlingerRequest
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import no.nav.omsorgsdager.vedtak.harEnEndeligStatus
import kotlin.reflect.KClass

internal fun <V: Vedtak> Route.BehandlingRoute(
    tilgangsstyring: Tilgangsstyring,
    path: String,
    vedtakType: KClass<V>,
    behandlingOperasjoner: BehandlingOperasjoner<V>) {

    route(path) {
        fun Set<Identitetsnummer>.opprettOperasjon() =
            Operasjon(type = Operasjon.Type.Endring, identitetsnummer = this, beskrivelse = "Opprette vedtak om ${vedtakType.simpleName}")

        post {
            val grunnlag = call.json()
            val behandlingId = grunnlag.map.getValue("behandlingId") as BehandlingId

            tilgangsstyring.verifiserTilgang(call, behandlingOperasjoner.preOpprett(grunnlag).opprettOperasjon())

            val eksisterendeBehandling = behandlingOperasjoner.hent(
                behandlingId = behandlingId
            )

            if (eksisterendeBehandling != null && eksisterendeBehandling.vedtak.harEnEndeligStatus()) {
                call.respond(HttpStatusCode.Conflict)
                return@post
            }

            val behandling = behandlingOperasjoner.opprett(
                grunnlag = grunnlag,
                correlationId = call.correlationId()
            )

            call.respond(HttpStatusCode.Created, HentBehandling.NøkkelinformasjonResponse(behandling))
        }

        fun Set<Identitetsnummer>.løseBehovOperasjon() =
            Operasjon(type = Operasjon.Type.Endring, identitetsnummer = this, beskrivelse = "Løse behov for ${vedtakType.simpleName}")

        patch("/{behandlingId}/løst") {
            val eksisterendeBehandling = behandlingOperasjoner.hent(
                behandlingId = call.behandlingId()
            )

            if (eksisterendeBehandling == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            if (eksisterendeBehandling.vedtak.harEnEndeligStatus()) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, eksisterendeBehandling.vedtak.involverteIdentitetsnummer.løseBehovOperasjon())

            val behandling = behandlingOperasjoner.løsninger(
                behandlingId = call.behandlingId(),
                grunnlag = call.json()
            )

            call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(behandling))
        }

        fun Set<Identitetsnummer>.innvilgeOperasjon() =
            Operasjon(type = Operasjon.Type.Endring, identitetsnummer = this, beskrivelse = "Innvilge vedtak om ${vedtakType.simpleName}")

        patch("/{behandlingId}/innvilget") {
            val eksisterendeBehandling = behandlingOperasjoner.hent(
                behandlingId = call.behandlingId()
            )
            if (eksisterendeBehandling == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, eksisterendeBehandling.vedtak.involverteIdentitetsnummer.innvilgeOperasjon())

            when {
                eksisterendeBehandling.vedtak.status == VedtakStatus.INNVILGET ->
                    call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(eksisterendeBehandling)).also { return@patch }
                eksisterendeBehandling.kanInnvilges -> proceed()
                else -> call.respond(HttpStatusCode.Conflict).also { return@patch }
            }

            val behandling = behandlingOperasjoner.innvilg(
                behandlingId = call.behandlingId(),
                tidspunkt = call.endreVedtakStatusTidspunkt()
            )

            // TODO: send till k9-vaktmester

            call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(behandling))
        }

        fun Set<Identitetsnummer>.avslåOperasjon() =
            Operasjon(type = Operasjon.Type.Endring, identitetsnummer = this, beskrivelse = "Avslå vedtak om ${vedtakType.simpleName}")

        patch("/{behandlingId}/avslått") {
            val eksisterendeBehandling = behandlingOperasjoner.hent(
                behandlingId = call.behandlingId()
            )

            if (eksisterendeBehandling == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, eksisterendeBehandling.vedtak.involverteIdentitetsnummer.avslåOperasjon())

            when {
                eksisterendeBehandling.vedtak.status == VedtakStatus.AVSLÅTT ->
                    call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(eksisterendeBehandling)).also { return@patch }
                eksisterendeBehandling.kanAvslås -> proceed()
                else -> call.respond(HttpStatusCode.Conflict).also { return@patch }
            }

            val behandling = behandlingOperasjoner.avslå(
                behandlingId = call.behandlingId(),
                tidspunkt = call.endreVedtakStatusTidspunkt()
            )

            // TODO: send till k9-vaktmester

            call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(behandling))
        }

        fun Set<Identitetsnummer>.forkasteOperasjon() =
            Operasjon(type = Operasjon.Type.Endring, identitetsnummer = this, beskrivelse = "Forkaste vedtak om ${vedtakType.simpleName}")

        patch("/{behandlingId}/forkastet") {
            val eksisterendeBehandling = behandlingOperasjoner.hent(
                behandlingId = call.behandlingId()
            )

            if (eksisterendeBehandling == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            tilgangsstyring.verifiserTilgang(call, eksisterendeBehandling.vedtak.involverteIdentitetsnummer.forkasteOperasjon())

            when {
                eksisterendeBehandling.vedtak.status == VedtakStatus.FORKASTET ->
                    call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(eksisterendeBehandling)).also { return@patch }
                eksisterendeBehandling.kanForkastes -> proceed()
                else -> call.respond(HttpStatusCode.Conflict).also { return@patch }
            }

            val behandling = behandlingOperasjoner.forkast(
                behandlingId = call.behandlingId(),
                tidspunkt = call.endreVedtakStatusTidspunkt()
            )

            call.respond(HttpStatusCode.OK, HentBehandling.NøkkelinformasjonResponse(behandling))
        }

        fun Set<Identitetsnummer>.henteOperasjon() =
            Operasjon(type = Operasjon.Type.Endring, identitetsnummer = this, beskrivelse = "Hente vedtak om ${vedtakType.simpleName}")

        suspend fun ApplicationCall.hentForBehandling(
            request: HentBehandling.Request) : HentBehandling.Response {

            val behandling = behandlingOperasjoner.hent(
                behandlingId = request.behandlingId!!
            ) ?: return HentBehandling.Response()

            if (!behandling.vedtak.erInnenforDatoer(fom = request.gyldigFraOgMed, tom = request.gyldigTilOgMed)) {
                return HentBehandling.Response()
            }

            tilgangsstyring.verifiserTilgang(this, behandling.vedtak.involverteIdentitetsnummer.henteOperasjon())

            return HentBehandling.Response(
                vedtak = listOf(behandlingOperasjoner.behandlingDto(behandling))
            )
        }

        suspend fun ApplicationCall.hentForSak(
            request: HentBehandling.Request) : HentBehandling.Response {
            val alle = behandlingOperasjoner.hentAlle(
                saksnummer = request.saksnummer!!
            )
            val gjeldendeVedtak = alle.map { it.vedtak }.gjeldendeVedtak()

            val identitetsnummer = gjeldendeVedtak.map { it.involverteIdentitetsnummer }.flatten().toSet()

            tilgangsstyring.verifiserTilgang(this, identitetsnummer.henteOperasjon())

            val vedtak = gjeldendeVedtak.filtrerPåDatoer(
                fom = request.gyldigFraOgMed,
                tom = request.gyldigTilOgMed
            ).map { gv -> alle.first { it.vedtak.behandlingId == gv.behandlingId }.copy(
                vedtak = gv
            )}.map { behandlingOperasjoner.behandlingDto(it) }

            return HentBehandling.Response(vedtak)
        }

        get {
            val request = call.hentBehandlingerRequest()
            when (request.hentForBehandling) {
                true -> call.respond(HttpStatusCode.OK, call.hentForBehandling(request))
                false -> call.respond(HttpStatusCode.OK, call.hentForSak(request))
            }
        }
    }
}