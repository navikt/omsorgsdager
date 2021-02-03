package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal data class Søker private constructor(
    val identitetsnummer: String,
    val fødselsdato: LocalDate) {
    internal constructor(node: ObjectNode) : this(
        identitetsnummer = node["identitetsnummer"].asText(),
        fødselsdato = node["fødselsdato"].asText().let { LocalDate.parse(it) }
    )
}
