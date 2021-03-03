package no.nav.omsorgsdager.rivers.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.PersonInfo
import no.nav.omsorgsdager.tid.Periode.Companion.dato

internal object HentPersonInfoMelding {
    internal const val HentPersonInfo = "HentPersonInfo"
    private const val PersonInfoKey = "@løsninger.$HentPersonInfo.personInfo"

    internal fun behovMedLøsning(personInfo: Map<AktørId, PersonInfo>) = Behov(
        navn = HentPersonInfo,
        input = mapOf(
            "aktørIder" to personInfo.keys.map { "$it" }
        )
    ) to personInfo.mapKeys { "${it.key}" }.mapValues { mapOf(
        "identitetsnummer" to "${it.value.identitetsnummer}",
        "fødselsdato" to "${it.value.fødselsdato}"
    )}.let { mapOf("personInfo" to it) }

    internal fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(PersonInfoKey)
    }

    internal fun hentLøsning(packet: JsonMessage): Map<AktørId, PersonInfo> {
        return (packet[PersonInfoKey] as ObjectNode)
            .fields()
            .asSequence()
            .map { Pair(it.key.somAktørId(), PersonInfo(
                identitetsnummer = it.value.get("identitetsnummer").asText().somIdentitetsnummer(),
                fødselsdato = it.value.get("fødselsdato").asText().dato()
            ))}
            .toMap()
    }
}