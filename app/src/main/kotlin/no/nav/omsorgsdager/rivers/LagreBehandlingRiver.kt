package no.nav.omsorgsdager.rivers

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgsdager.BehovssekvensId.Companion.somBehovssekvensId
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.behandling.BehandlingPersonInfo
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgsdager.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgsdager.rivers.meldinger.HentPersonInfoMelding
import no.nav.omsorgsdager.rivers.meldinger.HentPersonInfoMelding.HentPersonInfo
import org.slf4j.Logger

internal abstract class LagreBehandlingRiver(
    rapidsConnection: RapidsConnection,
    logger: Logger,
    private val behov: String,
    private val behandlingType: BehandlingType,
    private val behandlingStatus: BehandlingStatus,
    private val behandlingService: BehandlingService) : BehovssekvensPacketListener(logger = logger, mdcPaths = packetMdcPath(behov)) {

    private val BehovKey = "@behov.$behov"

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(behov)
                it.harLøsningPåBehov(HentOmsorgspengerSaksnummer, HentPersonInfo)
                it.require(BehovKey) { behov -> behov.requireObject() }
                HentOmsorgspengerSaksnummerMelding.validateLøsning(it)
                HentPersonInfoMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet)
        val personInfo = HentPersonInfoMelding.hentLøsning(packet)

        val grunnlag = (packet[BehovKey] as ObjectNode).somJson()

        val (nyBehandling, parter) = behandlingType.operasjoner.mapTilNyBehandling(
            behovssekvensId = id.somBehovssekvensId(),
            personInfo = saksnummer.mapValues { (identitetsnummer, saksnummer) -> BehandlingPersonInfo(
                saksnummer = saksnummer,
                aktørId = personInfo.entries.first { it.value.identitetsnummer == identitetsnummer}.key,
                fødselsdato = personInfo.entries.first { it.value.identitetsnummer == identitetsnummer}.value.fødselsdato
            )},
            grunnlag = grunnlag,
            behandlingStatus = behandlingStatus
        )

        behandlingService.lagre(
            behandling = nyBehandling,
            parter = parter
        )

        packet.leggTilLøsning(behov)
        logger.info("Behandling lagret")
        secureLogger.info("SuccessPacket=${packet.toJson()}")
        return true
    }
}