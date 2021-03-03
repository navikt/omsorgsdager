package no.nav.omsorgsdager.testutils.rapid

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgsdager.testutils.sisteMeldingSomJsonMessage
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer

internal fun TestRapid.mockHentOmsorgsdagerSaksnummer(identitetsnummer: Set<Identitetsnummer>) =
    sendTestMessage(sisteMeldingSomJsonMessage().leggTilLøsningPåHenteOmsorgspengerSaksnummer(identitetsnummer).toJson())

private fun JsonMessage.leggTilLøsningPåHenteOmsorgspengerSaksnummer(
    identitetsnummer: Set<Identitetsnummer>
) = leggTilLøsning(
    behov = HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
    løsning = mapOf(
        "saksnummer" to identitetsnummer.associateWith { it.somMocketOmsorgspengerSaksnummer().toString() }
    )
)