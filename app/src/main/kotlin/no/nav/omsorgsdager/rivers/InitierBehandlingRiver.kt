package no.nav.omsorgsdager.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import org.slf4j.Logger

internal abstract class InitierBehandlingRiver(
    rapidsConnection: RapidsConnection,
    logger: Logger,
    private val behov: String,
    identitetsnummerKeys: Set<String>
) : BehovssekvensPacketListener(logger = logger) {
    private val identitetsnummerKeysFullPath = identitetsnummerKeys.map { "@behov.$behov.$it" }

    init {
        logger.info("Henter identitetsnummer fra $identitetsnummerKeysFullPath")
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(behov)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummer)
                it.interestedIn(*identitetsnummerKeysFullPath.toTypedArray())
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val identitetsnummer = identitetsnummerKeysFullPath.map { key -> packet[key].asText().somIdentitetsnummer() }.toSet()
        logger.info("Legger til behov $HentOmsorgspengerSaksnummer for ${identitetsnummer.size} personer")

        packet.leggTilBehov(behov, HentOmsorgspengerSaksnummerMelding.behov(
            identitetsnummer = identitetsnummer
        ))

        return true
    }
}