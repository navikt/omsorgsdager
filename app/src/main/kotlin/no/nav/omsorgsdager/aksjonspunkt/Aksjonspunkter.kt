package no.nav.omsorgsdager.aksjonspunkt

internal data class Aksjonspunkter (
    internal val uløsteAksjonspunkter: Set<UløstAksjonspunkt>,
    internal val løsteAksjonspunkter: Set<LøstAksjonpunkt>
)