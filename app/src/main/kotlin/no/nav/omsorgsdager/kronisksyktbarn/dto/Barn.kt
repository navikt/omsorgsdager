package no.nav.omsorgsdager.kronisksyktbarn.dto

import no.nav.omsorgsdager.Identitetsnummer
import java.time.LocalDate

internal data class Barn private constructor(
    val identitetsnummer: Identitetsnummer? = null,
    val fødselsdato: LocalDate,
    val harSammeBosted: Boolean
)