package no.nav.omsorgsdager.rivers.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.PersonInfo
import no.nav.omsorgsdager.tid.Periode.Companion.dato

internal object HentUtvidetRettParterMelding {
    internal const val HentUtvidetRettParter = "HentUtvidetRettParter"
    private const val ParterKey = "@løsninger.$HentUtvidetRettParter.parter"

    internal fun behovMedLøsning(personInfo: Map<AktørId, PersonInfo>) = Behov(
        navn = HentUtvidetRettParter,
        input = mapOf(
            "aktørIder" to personInfo.keys.map { "$it" }
        )
    ) to personInfo.mapKeys { "${it.key}" }.mapValues { mapOf(
        "identitetsnummer" to "${it.value.identitetsnummer}",
        "fødselsdato" to "${it.value.fødselsdato}"
    )}.let { mapOf("parter" to it) }

    internal fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(ParterKey)
    }

    internal fun hentLøsning(packet: JsonMessage): Map<AktørId, PersonInfo> {
        return (packet[ParterKey] as ObjectNode)
            .fields()
            .asSequence()
            .map { Pair(it.key.somAktørId(), PersonInfo(
                identitetsnummer = it.value.get("identitetsnummer").asText().somIdentitetsnummer(),
                fødselsdato = it.value.get("fødselsdato").asText().dato()
            ))}
            .toMap()
    }
}