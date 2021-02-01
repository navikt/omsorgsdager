package no.nav.omsorgsdager.vedtak

internal data class VedtakResponse (
    val status: VedtakStatus,
    val uløsteAksjonspunkter: List<Aksjonspunkt>
    ) {

    fun toJson() = mapOf(
        "status" to status.name,
        "uløsteAksjonspunkter" to uløsteAksjonspunkter
            .associateBy { it.navn }
            .mapValues { Any() }
    )
}