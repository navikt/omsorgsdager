package no.nav.omsorgsdager.kronisksyktbarn.dto

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.behandling.Behandling
import no.nav.omsorgsdager.behov.dto.HentBehov.løsteDto
import no.nav.omsorgsdager.behov.dto.HentBehov.uløsteDto
import no.nav.omsorgsdager.kronisksyktbarn.KroniskSyktBarnVedtak
import no.nav.omsorgsdager.vedtak.VedtakStatus
import java.time.LocalDate

// TODO: Må denne være unik for kronisk sykt barn?
internal object HentKroniskSyktBarn {
    internal data class Response private constructor(
        val behandlingId: BehandlingId,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val status: VedtakStatus,
        val uløsteBehov: Map<String, Any>,
        val løsteBehov: Map<String, Any>,
        val grunnlag: Map<String, Any?>) {
        internal constructor(behandling: Behandling<KroniskSyktBarnVedtak>) : this(
            behandlingId = behandling.vedtak.behandlingId,
            gyldigFraOgMed = behandling.vedtak.periode.fom,
            gyldigTilOgMed = behandling.vedtak.periode.tom,
            status = behandling.vedtak.status,
            uløsteBehov = behandling.behov.uløsteBehov.uløsteDto(),
            løsteBehov = behandling.behov.løsteBehov.løsteDto(),
            grunnlag = behandling.vedtak.grunnlag.map
        )
    }
}