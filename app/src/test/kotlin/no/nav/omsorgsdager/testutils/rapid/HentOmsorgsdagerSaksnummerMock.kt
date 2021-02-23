package no.nav.omsorgsdager.testutils.rapid

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgsdager.testutils.sisteMeldingSomJsonMessage

internal fun TestRapid.mockHentOmsorgsdagerSaksnummer(identitetsnummer: Set<String>) =
    sendTestMessage(sisteMeldingSomJsonMessage().leggTilLøsningPåHenteOmsorgspengerSaksnummer(identitetsnummer).toJson())

private fun JsonMessage.leggTilLøsningPåHenteOmsorgspengerSaksnummer(
    identitetsnummer: Set<String>
) = leggTilLøsning(
    behov = HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
    løsning = mapOf(
        "saksnummer" to identitetsnummer.associateWith { "OP$it" }
    )
)