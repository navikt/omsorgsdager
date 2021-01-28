package no.nav.omsorgsdager.utvidetrett.dto

data class KronisktSyktBarnSoknadRequest(
    val saksnummer: String,
    val behandlingId: String,
    val mottatt: String,
    val søker: Søker,
    val barn: Barn
)