package no.nav.omsorgsdager.utvidetrett

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true, )
internal data class UtvidetRettRequestBody(
    val mottatt: String,
    val søker: Søker,
    val barn: Barn
)

data class Søker(
    val identitetsnummer: Identitetsnummer
)

data class Barn(
    val identitetsnummer: Identitetsnummer
)

internal typealias Identitetsnummer = String