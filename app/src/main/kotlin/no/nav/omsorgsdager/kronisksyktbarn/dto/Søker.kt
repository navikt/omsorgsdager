package no.nav.omsorgsdager.kronisksyktbarn.dto

import net.minidev.json.annotate.JsonIgnore
import no.nav.omsorgsdager.Identitetsnummer
import java.time.LocalDate

internal data class Søker constructor(
    val identitetsnummer: Identitetsnummer,
    val fødselsdato: LocalDate) {
    @get:JsonIgnore
    internal val sisteDagSøkerHarRettTilOmsorgsdager = fødselsdato.plusYears(70).minusDays(1)
}
