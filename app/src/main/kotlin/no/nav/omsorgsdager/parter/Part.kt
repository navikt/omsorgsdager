package no.nav.omsorgsdager.parter

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.person.AktørId
import java.time.LocalDate

internal interface Part {
    val aktørId: AktørId
    val identitetsnummer: Identitetsnummer
    val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
}

internal data class Barn(
    override val aktørId: AktørId,
    override val identitetsnummer: Identitetsnummer,
    override val omsorgspengerSaksnummer: OmsorgspengerSaksnummer,
    internal val fødselsdato: LocalDate
) : Part

internal data class Søker(
    override val aktørId: AktørId,
    override val identitetsnummer: Identitetsnummer,
    override val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
) : Part

internal data class Motpart(
    override val aktørId: AktørId,
    override val identitetsnummer: Identitetsnummer,
    override val omsorgspengerSaksnummer: OmsorgspengerSaksnummer
) : Part