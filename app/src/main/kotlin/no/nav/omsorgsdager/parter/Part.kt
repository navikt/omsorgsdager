package no.nav.omsorgsdager.parter

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import java.time.LocalDate

internal interface Part {
    val identitetsnummer: Identitetsnummer
    val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
}

internal data class Barn(
    override val identitetsnummer: Identitetsnummer,
    override val omsorgspengerSaksnummer: OmsorgspengerSaksnummer,
    internal val fødselsdato: LocalDate
) : Part

internal data class Søker(
    override val identitetsnummer: Identitetsnummer,
    override val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
) : Part

internal data class Motpart(
    override val identitetsnummer: Identitetsnummer,
    override val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
) : Part