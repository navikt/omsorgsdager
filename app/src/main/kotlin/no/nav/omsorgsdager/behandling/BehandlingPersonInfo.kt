package no.nav.omsorgsdager.behandling

import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.person.AktørId
import java.time.LocalDate

internal data class BehandlingPersonInfo (
    internal val aktørId: AktørId,
    internal val fødselsdato: LocalDate,
    internal val saksnummer: OmsorgspengerSaksnummer
)