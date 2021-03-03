package no.nav.omsorgsdager.person

import no.nav.omsorgsdager.Identitetsnummer
import java.time.LocalDate

internal data class PersonInfo (
    internal val identitetsnummer: Identitetsnummer,
    internal val fødselsdato: LocalDate
)