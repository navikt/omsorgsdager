package no.nav.omsorgsdager.rivers.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer

internal object HentOmsorgspengerSaksnummerMelding {
    internal const val HentOmsorgspengerSaksnummer = "HentOmsorgspengerSaksnummer"
    private const val SaksnummerKey = "@løsninger.$HentOmsorgspengerSaksnummer.saksnummer"

    internal fun behov(identitetsnummer: Set<Identitetsnummer>) = Behov(
        navn = HentOmsorgspengerSaksnummer,
        input = mapOf(
            "identitetsnummer" to identitetsnummer.map { it.toString() }
        )
    )

    fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(SaksnummerKey)
    }

    fun hentLøsning(packet: JsonMessage): Map<Identitetsnummer, OmsorgspengerSaksnummer> {
        return (packet[SaksnummerKey] as ObjectNode)
            .fields()
            .asSequence()
            .map { Pair(it.key.somIdentitetsnummer(), it.value.asText().somOmsorgspengerSaksnummer())}
            .toMap()
    }
}