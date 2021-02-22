package no.nav.omsorgsdager.parter

import no.nav.omsorgsdager.Identitetsnummer
import java.time.LocalDate

interface Part

internal data class Barn(
    internal val identitetsnummer: Identitetsnummer?,
    internal val fødselsdato: LocalDate
) : Part

internal data class Søker(
    internal val identitetsnummer: Identitetsnummer
) : Part

internal data class Motpart(
    internal val identitetsnummer: Identitetsnummer
) : Part