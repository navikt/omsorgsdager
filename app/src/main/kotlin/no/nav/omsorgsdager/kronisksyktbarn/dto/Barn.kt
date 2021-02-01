package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

data class Barn(
    val identitetsnummer: String,
    val fødselsdato: LocalDate
) {
    constructor(node: ObjectNode) : this(
        identitetsnummer = node["identitetsnummer"].asText(),
        fødselsdato = node["fødselsdato"].asText().let { LocalDate.parse(it) },
    )
}
