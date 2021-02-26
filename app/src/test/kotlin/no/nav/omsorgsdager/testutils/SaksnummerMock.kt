package no.nav.omsorgsdager.testutils

import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import java.util.*

internal fun Any.somMocketOmsorgspengerSaksnummer() = "OP$this".somOmsorgspengerSaksnummer()
internal fun mocketK9Saksnummer() = UUID.randomUUID().toString().takeLast(10).somK9Saksnummer()
