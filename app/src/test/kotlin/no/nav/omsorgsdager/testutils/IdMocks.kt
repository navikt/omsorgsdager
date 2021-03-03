package no.nav.omsorgsdager.testutils

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import no.nav.omsorgsdager.person.AktørId
import java.util.*

internal fun AktørId.somMockedIdentetsnummer() = "9$this".somIdentitetsnummer()
internal fun Identitetsnummer.somMocketOmsorgspengerSaksnummer() = "OP$this".somOmsorgspengerSaksnummer()
internal fun mocketK9Saksnummer() = UUID.randomUUID().toString().takeLast(10).somK9Saksnummer()
