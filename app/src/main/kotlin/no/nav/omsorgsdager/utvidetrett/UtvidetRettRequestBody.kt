package no.nav.omsorgsdager.utvidetrett

internal data class UtvidetRettRequestBody(
    val mottattDato: String,
    val søker: Identitetsnummer,
    val barn: List<Identitetsnummer>
)

internal typealias Identitetsnummer = String