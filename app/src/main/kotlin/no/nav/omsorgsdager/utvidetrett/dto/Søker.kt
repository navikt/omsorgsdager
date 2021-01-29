package no.nav.omsorgsdager.utvidetrett.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

data class Søker(
    val identitetsnummer: String,
    val fødselsdato: LocalDate,
    val jobberINorge: Boolean
) {
    constructor(node: ObjectNode) : this(
        identitetsnummer = node["identitetsnummer"].asText(),
        fødselsdato = node["fødselsdato"].asText().let { LocalDate.parse(it) },
        jobberINorge = node["jobberINorge"].asBoolean()
    )
}
