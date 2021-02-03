package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgsdager.Identitetsnummer
import java.time.LocalDate

internal data class Søker constructor(
    val identitetsnummer: Identitetsnummer,
    val fødselsdato: LocalDate) {
    internal constructor(node: ObjectNode) : this(
        identitetsnummer = node["identitetsnummer"].asText(),
        fødselsdato = node["fødselsdato"].asText().let { LocalDate.parse(it) }
    )

    val sisteDagSøkerHarRettTilOmsorgsdager = fødselsdato.plusYears(70).minusDays(1)

}
