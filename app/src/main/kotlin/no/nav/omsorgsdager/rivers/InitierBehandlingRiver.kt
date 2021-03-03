package no.nav.omsorgsdager.rivers

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.river.*
import no.nav.omsorgsdager.CorrelationId.Companion.somCorrelationId
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.PersonInfoGateway
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgsdager.rivers.meldinger.HentPersonInfoMelding
import org.slf4j.Logger

internal abstract class InitierBehandlingRiver(
    rapidsConnection: RapidsConnection,
    logger: Logger,
    private val behov: String,
    private val støttedeVersjoner: Set<String>,
    aktørIdKeys: Set<String>,
    private val personInfoGateway: PersonInfoGateway
) : BehovssekvensPacketListener(logger = logger) {
    private val aktørIdKeysFullPath = aktørIdKeys.map { "@behov.$behov.$it" }
    private val versjonKey = "@behov.$behov.versjon"

    init {
        logger.info("Henter aktørIder fra $aktørIdKeysFullPath")
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(behov)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummer)
                it.interestedIn(*aktørIdKeysFullPath.toTypedArray())
                it.interestedIn(versjonKey)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val versjon = packet[versjonKey].asText()
        val erStøttetVersjon = versjon in støttedeVersjoner
        logger.info("Versjon=[$versjon], ErStøttetVersjon=[$erStøttetVersjon]")
        return erStøttetVersjon
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet[Behovsformat.CorrelationId].asText().somCorrelationId()
        val aktørIder = aktørIdKeysFullPath.map { key -> packet[key].asText().somAktørId() }.toSet()
        logger.info("AntallInvolverteAktører=[${aktørIder.size}]")

        logger.info("Henter personinfo for involverte aktører.")
        val personInfo = runBlocking { personInfoGateway.hent(aktørIder = aktørIder, correlationId = correlationId) }
        packet.leggTilBehovMedLøsninger(behov, HentPersonInfoMelding.behovMedLøsning(personInfo))

        logger.info("Legger til behov $HentOmsorgspengerSaksnummer")
        packet.leggTilBehov(behov, HentOmsorgspengerSaksnummerMelding.behov(
            identitetsnummer = personInfo.values.map {it.identitetsnummer}.toSet()
        ))

        return true
    }
}