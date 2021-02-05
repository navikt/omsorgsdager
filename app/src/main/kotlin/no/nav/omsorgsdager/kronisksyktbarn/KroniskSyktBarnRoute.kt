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
import no.nav.omsorgsdager.behov.UløstBehov
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
import no.nav.omsorgsdager.vedtak.dto.EndreVedtakStatus.endreVedtakStatusTidspunkt
import no.nav.omsorgsdager.vedtak.dto.VedtakNøkkelinformasjon
import no.nav.omsorgsdager.vedtak.dto.VedtakNøkkelinformasjon.kanAvslås
import no.nav.omsorgsdager.vedtak.dto.VedtakNøkkelinformasjon.kanForkastes
import no.nav.omsorgsdager.vedtak.dto.VedtakNøkkelinformasjon.kanInnvilges
import no.nav.omsorgsdager.vedtak.harEnEndeligStatus

internal fun Route.KroniskSyktBarnRoute(
    tilgangsstyring: Tilgangsstyring,
    kroniskSyktBarnRepository: KroniskSyktBarnRepository) {

    route("/kroniskt-sykt-barn") {

        post {
            val request = call.objectNode()
            val grunnlag = OpprettKroniskSyktBarn.Grunnlag(request)

            tilgangsstyring.verifiserTilgang(call, Operasjoner.NyttVedtakKroniskSyktBarn.copy(
                identitetsnummer = grunnlag.involverteIdentitetsnummer
            ))

            val eksisterendeVedtak = kroniskSyktBarnRepository.hent(behandlingId = grunnlag.behandlingId)
            if (eksisterendeVedtak != null && eksisterendeVedtak.first.harEnEndeligStatus()) {
                call.respond(HttpStatusCode.Conflict)
                return@post
            }

            val tilOgMed = minOf(
                grunnlag.barn.fødselsdato.sisteDagIÅretOm18År(),
                grunnlag.søker.sisteDagSøkerHarRettTilOmsorgsdager
            )

            val uløsteBehov = setOf(UløstBehov(navn = "LEGEERKLÆRING"))
            val (vedtak, behov) = kroniskSyktBarnRepository.nyttVedtak(
                vedtak = KroniskSyktBarnVedtak(
                    saksnummer = grunnlag.saksnummer,
                    behandlingId = grunnlag.behandlingId,
                    status = VedtakStatus.FORESLÅTT,
                    statusSistEndret = grunnlag.tidspunkt,
                    søker = grunnlag.søker,
                    barn = grunnlag.barn,
                    periode = Periode(
                        fom = grunnlag.søknadMottatt.toLocalDateOslo(),
                        tom = tilOgMed
                    )
                ),
                uløsteBehov = uløsteBehov
            )

            call.respond(HttpStatusCode.Created, VedtakNøkkelinformasjon.Response(
                vedtak to behov
            ))
        }

        patch("/{behandlingId}/løst") {
            val behandlingId = call.behandlingId()
            val vedtakOgBehov = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgBehov == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            if (vedtakOgBehov.first.harEnEndeligStatus()) {
                call.respond(HttpStatusCode.Conflict)
                return@patch
            }

            val request = call.objectNode()
            val løsteBehov = request.map<LøsKroniskSyktBarnBehov.Request>()

            tilgangsstyring.verifiserTilgang(call, Operasjoner.LøseBehovKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgBehov.first.involverteIdentitetsnummer
            ))

            val (vedtak, behov) = kroniskSyktBarnRepository.løsteBehov(
                behandlingId = behandlingId,
                løsteBehov = løsteBehov.løsteBehov
            )

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak to behov
            ))
        }

        patch("/{behandlingId}/innvilget") {
            val behandlingId = call.behandlingId()
            val vedtakOgBehov = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgBehov == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            when {
                vedtakOgBehov.first.status == VedtakStatus.INNVILGET ->
                    call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(vedtakOgBehov)).also { return@patch }
                vedtakOgBehov.kanInnvilges() -> proceed()
                else -> call.respond(HttpStatusCode.Conflict).also { return@patch }
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.InnvilgeKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgBehov.first.involverteIdentitetsnummer
            ))

            val (innvilgetVedtak, innvilgetBehov) = kroniskSyktBarnRepository.endreStatus(
                behandlingId = behandlingId,
                status = VedtakStatus.INNVILGET,
                tidspunkt = call.endreVedtakStatusTidspunkt()
            )

            // TODO: send till k9-vaktmester

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                innvilgetVedtak to innvilgetBehov
            ))
        }

        patch("/{behandlingId}/forkastet") {
            val behandlingId = call.behandlingId()
            val vedtakOgBehov = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgBehov == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            when {
                vedtakOgBehov.first.status == VedtakStatus.FORKASTET ->
                    call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(vedtakOgBehov)).also { return@patch }
                vedtakOgBehov.kanForkastes() -> proceed()
                else -> call.respond(HttpStatusCode.Conflict).also { return@patch }
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.ForkastKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgBehov.first.involverteIdentitetsnummer
            ))

            val (vedtak, behov) = kroniskSyktBarnRepository.endreStatus(
                behandlingId = behandlingId,
                status = VedtakStatus.FORKASTET,
                tidspunkt = call.endreVedtakStatusTidspunkt()
            )

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak to behov
            ))
        }

        patch("/{behandlingId}/avslått") {
            val behandlingId = call.behandlingId()
            val vedtakOgBehov = kroniskSyktBarnRepository.hent(behandlingId = behandlingId)
            if (vedtakOgBehov == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            when {
                vedtakOgBehov.first.status == VedtakStatus.AVSLÅTT ->
                    call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(vedtakOgBehov)).also { return@patch }
                vedtakOgBehov.kanAvslås() -> proceed()
                else -> call.respond(HttpStatusCode.Conflict).also { return@patch }
            }

            tilgangsstyring.verifiserTilgang(call, Operasjoner.AvslåKroniskSyktBarn.copy(
                identitetsnummer = vedtakOgBehov.first.involverteIdentitetsnummer
            ))

            val (vedtak, behov) = kroniskSyktBarnRepository.endreStatus(
                behandlingId = behandlingId,
                status = VedtakStatus.AVSLÅTT,
                tidspunkt = call.endreVedtakStatusTidspunkt()
            )

            call.respond(HttpStatusCode.OK, VedtakNøkkelinformasjon.Response(
                vedtak to behov
            ))
        }

        suspend fun ApplicationCall.hentForBehandling(
            request: HentKroniskSyktBarn.Request) : HentKroniskSyktBarn.Response {
            val (vedtak, behov) = kroniskSyktBarnRepository.hent(behandlingId = request.behandlingId!!) ?: return HentKroniskSyktBarn.Response()

            if (!vedtak.erInnenforDatoer(fom = request.gyldigFraOgMed, tom = request.gyldigTilOgMed)) {
                return HentKroniskSyktBarn.Response()
            }

            tilgangsstyring.verifiserTilgang(this, Operasjoner.HenteKroniskSyktBarnBehandling.copy(
                identitetsnummer = vedtak.involverteIdentitetsnummer
            ))

            return HentKroniskSyktBarn.Response(
                vedtak = listOf(HentKroniskSyktBarn.Vedtak(
                    vedtak = vedtak,
                    behov = behov
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
                behov = alle.first { it.first.behandlingId == gv.behandlingId }.second
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
    val NyttVedtakKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Endring,
        beskrivelse = "Registrere nytt vedtak for kronisk sykt barn",
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

    val LøseBehovKroniskSyktBarn = Operasjon(
        type = Operasjon.Type.Visning,
        beskrivelse = "Løse behov for kronisk sykt barn",
        identitetsnummer = setOf()
    )
}