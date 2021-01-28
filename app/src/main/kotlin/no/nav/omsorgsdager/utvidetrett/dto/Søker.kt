package no.nav.omsorgsdager.utvidetrett.dto

data class Søker(
    val identitetsnummer: String,
    val fødselsdato: String,
    val jobberINorge: Boolean
)
