package no.nav.omsorgsdager.vedtak

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdager.CorrelationId.Companion.correlationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.json
import no.nav.omsorgsdager.SecureLogger
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.periodeOrNull
import no.nav.omsorgsdager.tilgangsstyring.Operasjon
import no.nav.omsorgsdager.tilgangsstyring.Tilgangsstyring
import java.time.LocalDate

internal fun Route.InnvilgedeVedtakApis(
    tilgangsstyring: Tilgangsstyring,
    innvilgedeVedtakService: InnvilgedeVedtakService) {

    fun Pair<Identitetsnummer, Periode>.henteInnvilgedeVedtakOmUtvidetRettFor() =
        Operasjon(type = Operasjon.Type.Visning, identitetsnummer = setOf(first), beskrivelse = "Hente innvilgede vedtak om utvidet rett for perioden $second")

    fun Json.identitetsnummerOgPeriode() = kotlin.runCatching {
        val identitetsnummer = map["identitetsnummer"]?.toString()?.somIdentitetsnummer()
        val fom = map["fom"]?.toString()?.let { LocalDate.parse(it) }
        val tom = map["tom"]?.toString()?.let { LocalDate.parse(it) }
        val periode = (fom to tom).periodeOrNull()?.sanitized()
        requireNotNull(identitetsnummer) to requireNotNull(periode)
    }.fold(
        onSuccess = { it },
        onFailure = {
            SecureLogger.warn("Ugylidg request for henting av innvilgede vedtak: $this")
            null
        }
    )

    post("innvilgede-vedtak-utvidet-rett") {
        val json = call.json()
        val correlationId = call.correlationId()

        val identitetsnummerOgPeriode = json.identitetsnummerOgPeriode()

        if (identitetsnummerOgPeriode == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        tilgangsstyring.verifiserTilgang(
            call = call,
            operasjon = identitetsnummerOgPeriode.henteInnvilgedeVedtakOmUtvidetRettFor()
        )

        val innvilgedeVedtak = innvilgedeVedtakService.hentInnvilgedeVedtak(
            identitetsnummer = identitetsnummerOgPeriode.first,
            periode = identitetsnummerOgPeriode.second,
            correlationId = correlationId
        )

        call.respond(innvilgedeVedtak)
    }
}