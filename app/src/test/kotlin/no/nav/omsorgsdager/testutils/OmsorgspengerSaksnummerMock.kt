package no.nav.omsorgsdager.testutils

import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnumer

internal fun Any.somMocketOmsorgspengerSaksnummer() = "OP$this".somOmsorgspengerSaksnumer()
