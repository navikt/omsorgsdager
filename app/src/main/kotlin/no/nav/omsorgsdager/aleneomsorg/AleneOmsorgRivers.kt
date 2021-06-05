package no.nav.omsorgsdager.aleneomsorg

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.person.PersonInfoGateway
import no.nav.omsorgsdager.rivers.InitierBehandlingRiver
import no.nav.omsorgsdager.rivers.LagreBehandlingRiver
import org.slf4j.LoggerFactory

private object Behov {
    const val InnvilgetAleneOmsorg = "InnvilgetAleneOmsorg"
    const val AvslåttAleneOmsorg = "AvslåttAleneOmsorg"
    val aktørIdKeys = setOf(
        "søker.aktørId",
        "barn.aktørId"
    )
    val støttedeVersjoner = setOf("1.0.0")
}

internal class InitierInnvilgetAleneOmsorgRiver(
    rapidsConnection: RapidsConnection,
    personInfoGateway: PersonInfoGateway
) : InitierBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(InitierInnvilgetAleneOmsorgRiver::class.java),
    behov = Behov.InnvilgetAleneOmsorg,
    aktørIdKeys = Behov.aktørIdKeys,
    støttedeVersjoner = Behov.støttedeVersjoner,
    personInfoGateway = personInfoGateway
)

internal class LagreInnvilgetAleneOmsorgRiver(
    rapidsConnection: RapidsConnection,
    behandlingService: BehandlingService
) : LagreBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(LagreInnvilgetAleneOmsorgRiver::class.java),
    behov = Behov.InnvilgetAleneOmsorg,
    behandlingType = BehandlingType.ALENE_OMSORG,
    behandlingStatus = BehandlingStatus.INNVILGET,
    behandlingService = behandlingService
)


internal class InitierAvslåttAleneOmsorgRiver(
    rapidsConnection: RapidsConnection,
    personInfoGateway: PersonInfoGateway
) : InitierBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(InitierAvslåttAleneOmsorgRiver::class.java),
    behov = Behov.AvslåttAleneOmsorg,
    aktørIdKeys = Behov.aktørIdKeys,
    støttedeVersjoner = Behov.støttedeVersjoner,
    personInfoGateway = personInfoGateway
)

internal class LagreAvslåttAleneOmsorgRiver(
    rapidsConnection: RapidsConnection,
    behandlingService: BehandlingService
) : LagreBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(LagreAvslåttAleneOmsorgRiver::class.java),
    behov = Behov.AvslåttAleneOmsorg,
    behandlingType = BehandlingType.ALENE_OMSORG,
    behandlingStatus = BehandlingStatus.AVSLÅTT,
    behandlingService = behandlingService
)