package no.nav.omsorgsdager.kronisksyktbarn.dto

import io.ktor.application.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import java.time.LocalDate

internal data class HentKroniskSyktBarnRequest private constructor(
    internal val saksnummer: Saksnummer? = null,
    internal val behandlingId: BehandlingId? = null,
    internal val gyldigFraOgMed: LocalDate? = null,
    internal val gyldigTilOgMed: LocalDate? = null) {
    internal val hentForBehandlingId = behandlingId != null
    internal companion object {
        internal fun ApplicationCall.hentKroniskSyktBarnRequest() = HentKroniskSyktBarnRequest(
            saksnummer = request.queryParameters["saksnummer"],
            behandlingId = request.queryParameters["behandlingId"],
            gyldigFraOgMed = request.queryParameters["gyldigFraOgMed"]?.let { LocalDate.parse(it) },
            gyldigTilOgMed = request.queryParameters["gyldigTilOgMed"]?.let { LocalDate.parse(it) }
        ).also { when {
            it.saksnummer == null && it.behandlingId == null -> throw IllegalArgumentException(
                "Må sette enten saksnummer eller behandlingId, ingen er satt ($it)"
            )
            it.saksnummer != null && it.behandlingId != null -> throw IllegalArgumentException(
                "Må sette enten saksnummer eller behandlingId, begge er satt($it)"
            )
        }}
    }
}