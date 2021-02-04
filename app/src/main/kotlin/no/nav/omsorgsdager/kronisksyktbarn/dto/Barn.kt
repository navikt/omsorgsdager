package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgsdager.Identitetsnummer
import java.time.LocalDate

internal data class Barn private constructor(
    val identitetsnummer: Identitetsnummer ?= null,
    val fødselsdato: LocalDate,
    val harSammeBosted: Boolean) {

    internal constructor(node: ObjectNode) : this(
        identitetsnummer = node["identitetsnummer"]?.asText(),
        fødselsdato = node["fødselsdato"].asText().let { LocalDate.parse(it) },
        harSammeBosted = node["harSammeBosted"].asBoolean()
    )
}
