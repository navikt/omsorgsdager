package no.nav.omsorgsdager.rivers

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import org.slf4j.Logger

internal abstract class LagreBehandlingRiver(
    rapidsConnection: RapidsConnection,
    logger: Logger,
    private val behov: String,
    private val behandlingType: BehandlingType,
    private val behandlingStatus: BehandlingStatus,
    private val behandlingService: BehandlingService) : BehovssekvensPacketListener(logger = logger) {

    // TODO: Lagre behovssekvensid ?

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(behov)
                it.harLøsningPåBehov(HentOmsorgspengerSaksnummer)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet)
        val grunnlag = (packet["@behov.$behov"] as ObjectNode).somJson()

        val (nyBehandling, parter) = behandlingType.operasjoner.mapTilNyBehandling(
            saksnummer = saksnummer,
            grunnlag = grunnlag,
            behandlingStatus = behandlingStatus
        )

        behandlingService.lagre(
            behandling = nyBehandling,
            parter = parter
        )

        return true
    }
}