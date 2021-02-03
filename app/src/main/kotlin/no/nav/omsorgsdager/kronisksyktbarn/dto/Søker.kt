package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgsdager.tid.Periode.Companion.erFørEllerLik
import java.time.LocalDate

data class Søker(
    val identitetsnummer: String,
    val fødselsdato: LocalDate) {
    constructor(node: ObjectNode) : this(
        identitetsnummer = node["identitetsnummer"].asText(),
        fødselsdato = node["fødselsdato"].asText().let { LocalDate.parse(it) }
    )

    fun erOver70EtterDato(dato: LocalDate) = ålderVidDato(dato) >= 70

    fun ålderVidDato(dato: LocalDate): Int {
        if(dato.erFørEllerLik(this.fødselsdato)) return 0

        val søkersAlder = dato.year - fødselsdato.year

        return if(LocalDate.now().dayOfYear < fødselsdato.dayOfYear)
            søkersAlder.minus(1)
        else
            søkersAlder
    }
}
