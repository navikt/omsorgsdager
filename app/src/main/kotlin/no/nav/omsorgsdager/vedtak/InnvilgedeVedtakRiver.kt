package no.nav.omsorgsdager.vedtak

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgsdager.CorrelationId.Companion.somCorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.tid.Periode
import org.slf4j.LoggerFactory

internal class InnvilgedeVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val innvilgedeVedtakService: InnvilgedeVedtakService
) : BehovssekvensPacketListener(logger = logger) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(Behov)
                it.interestedIn(IdentitetsnummerKey, FomKey, TomKey)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Henter innvilgede vedtak for utvidet rett.")
        val innvilgedeVedtak = runBlocking { innvilgedeVedtakService.hentInnvilgedeVedtak(
            identitetsnummer = packet.identietsnummer(),
            periode = packet.periode(),
            correlationId = packet.correlationId()
        )}

        packet.leggTilLøsning(Behov, Json(innvilgedeVedtak).map)

        return true
    }

    private fun JsonMessage.periode() = Periode(fom = get(FomKey).asLocalDate(), tom = get(TomKey).asLocalDate())
    private fun JsonMessage.identietsnummer() = get(IdentitetsnummerKey).asText().somIdentitetsnummer()
    private fun JsonMessage.correlationId() = get("@correlationId").asText().somCorrelationId()

    private companion object {
        private const val Behov = "HentUtvidetRettVedtakV2"
        private const val FomKey = "@behov.$Behov.fom"
        private const val TomKey = "@behov.$Behov.tom"
        private const val IdentitetsnummerKey = "@behov.$Behov.identitetsnummer"
        private val logger = LoggerFactory.getLogger(InnvilgedeVedtakRiver::class.java)
    }
}