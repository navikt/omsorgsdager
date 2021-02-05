package no.nav.omsorgsdager.kronisksyktbarn.dto

import io.ktor.application.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.behov.Behov
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnVedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.LocalDate

internal object HentKroniskSyktBarn {
    internal data class Request private constructor(
        internal val saksnummer: Saksnummer? = null,
        internal val behandlingId: BehandlingId? = null,
        internal val gyldigFraOgMed: LocalDate? = null,
        internal val gyldigTilOgMed: LocalDate? = null) {
        internal val hentForBehandling = behandlingId != null
        internal companion object {
            internal fun ApplicationCall.hentKroniskSyktBarnRequest() = Request(
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

    internal data class Vedtak private constructor(
        val barn: Barn,
        val behandlingId: BehandlingId,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val status: VedtakStatus,
        val uløsteBehov: Map<String, Any>,
        val løsteBehov: Map<String, Any>) {
        internal constructor(vedtak: KroniskSyktBarnVedtak, behov: Behov) : this(
            barn = vedtak.barn,
            behandlingId = vedtak.behandlingId,
            gyldigFraOgMed = vedtak.periode.fom,
            gyldigTilOgMed = vedtak.periode.tom,
            status = vedtak.status,
            uløsteBehov = behov.uløsteBehov
                .associateBy { it.navn }
                .mapValues { Any() },
            løsteBehov = behov.løsteBehov
                .associateBy { it.navn }
                .mapValues { it.value.løsning.map }
        )
    }

    internal data class Response(
        val vedtak: List<Vedtak> = emptyList()
    )
}