package no.nav.omsorgsdager.vedtak

import no.nav.omsorgsdager.aksjonspunkt.UløstAksjonspunkt

internal data class VedtakResponse (
    val status: VedtakStatus,
    val uløsteAksjonspunkter: Set<UløstAksjonspunkt>) {

    fun toJson() = mapOf(
        "status" to status.name,
        "uløsteAksjonspunkter" to uløsteAksjonspunkter
            .associateBy { it.navn }
            .mapValues { Any() }
    )
}