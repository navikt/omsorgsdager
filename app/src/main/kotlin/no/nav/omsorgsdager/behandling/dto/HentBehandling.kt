package no.nav.omsorgsdager.behandling.dto

import io.ktor.application.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behov.uløsteDto
import no.nav.omsorgsdager.vedtak.Vedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import no.nav.omsorgsdager.vedtak.statusDto
import java.time.LocalDate

internal object HentBehandling {
    internal data class Request private constructor(
        internal val saksnummer: Saksnummer? = null,
        internal val behandlingId: BehandlingId? = null,
        internal val gyldigFraOgMed: LocalDate? = null,
        internal val gyldigTilOgMed: LocalDate? = null) {
        internal val hentForBehandling = behandlingId != null
        internal companion object {
            internal fun ApplicationCall.hentBehandlingerRequest() = Request(
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

    internal data class Response(
        val vedtak: List<Any> = emptyList()
    )

    internal data class NøkkelinformasjonResponse private constructor(
        val status: VedtakStatus,
        val potensielleStatuser: Map<String, Any>,
        val uløsteBehov: Map<String, Any>) {
        internal constructor (behandling: Behandling<out Vedtak>) : this(
            status = behandling.vedtak.status,
            potensielleStatuser = behandling.potensielleStatuser.statusDto(),
            uløsteBehov = behandling.behov.uløsteBehov.uløsteDto()
        )
    }
}