package no.nav.omsorgsdager.utvidetrett

internal data class UtvidetRettRequestBody(
    val mottatt: String,
    val söker: Identitetsnummer,
    val barn: List<Identitetsnummer>
)

internal typealias Identitetsnummer = String