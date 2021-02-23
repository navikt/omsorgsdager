package no.nav.omsorgsdager.parter

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import java.time.LocalDate

interface Part

internal data class Barn(
    internal val identitetsnummer: Identitetsnummer?,
    internal val fødselsdato: LocalDate
) : Part

internal data class Søker(
    internal val identitetsnummer: Identitetsnummer,
    internal val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
) : Part

internal data class Motpart(
    internal val identitetsnummer: Identitetsnummer,
    internal val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
) : Part