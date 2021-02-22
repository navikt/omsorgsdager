package no.nav.omsorgsdager.midlertidigalene

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.rivers.InitierBehandlingRiver
import no.nav.omsorgsdager.rivers.LagreBehandlingRiver
import org.slf4j.LoggerFactory

private object Behov {
    const val InnvilgetMidlertidigAlene = "InnvilgetMidlertidigAlene"
    const val AvslåttMidlertidigAlene = "AvslåttMidlertidigAlene"
    val identitetsnummerKeys = setOf(
        "søker.identitetsnummer",
        "annenForelder.identitetsnummer"
    )
}

internal class InitierInnvilgetMidlertidigAleneRiver(
    rapidsConnection: RapidsConnection
) : InitierBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(InitierInnvilgetMidlertidigAleneRiver::class.java),
    behov = Behov.InnvilgetMidlertidigAlene,
    identitetsnummerKeys = Behov.identitetsnummerKeys
)

internal class LagreInnvilgetMidlertidigAleneRiver(
    rapidsConnection: RapidsConnection,
    behandlingService: BehandlingService
) : LagreBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(LagreInnvilgetMidlertidigAleneRiver::class.java),
    behov = Behov.InnvilgetMidlertidigAlene,
    behandlingType = BehandlingType.MIDLERTIDIG_ALENE,
    behandlingStatus = BehandlingStatus.INNVILGET,
    behandlingService = behandlingService
)


internal class InitierAvslåttMidlertidigAleneRiver(
    rapidsConnection: RapidsConnection
) : InitierBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(InitierAvslåttMidlertidigAleneRiver::class.java),
    behov = Behov.AvslåttMidlertidigAlene,
    identitetsnummerKeys = Behov.identitetsnummerKeys
)

internal class LagreAvslåttMidlertidigAleneRiver(
    rapidsConnection: RapidsConnection,
    behandlingService: BehandlingService
) : LagreBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(LagreAvslåttMidlertidigAleneRiver::class.java),
    behov = Behov.AvslåttMidlertidigAlene,
    behandlingType = BehandlingType.MIDLERTIDIG_ALENE,
    behandlingStatus = BehandlingStatus.AVSLÅTT,
    behandlingService = behandlingService
)