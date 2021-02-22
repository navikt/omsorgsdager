package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgsdager.behandling.BehandlingService
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.rivers.InitierBehandlingRiver
import no.nav.omsorgsdager.rivers.LagreBehandlingRiver
import org.slf4j.LoggerFactory

private object Behov {
    const val InnvilgetKroniskSyktBarn = "InnvilgetKroniskSyktBarn"
    const val AvslåttKroniskSyktBarn = "AvslåttKroniskSyktBarn"
    val identitetsnummerKeys = setOf(
        "søker.identitetsnummer"
    )
}

internal class InitierInnvilgetKroniskSyktBarnRiver(
    rapidsConnection: RapidsConnection
) : InitierBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(InitierInnvilgetKroniskSyktBarnRiver::class.java),
    behov = Behov.InnvilgetKroniskSyktBarn,
    identitetsnummerKeys = Behov.identitetsnummerKeys
)

internal class LagreInnvilgetKroniskSyktBarnRiver(
    rapidsConnection: RapidsConnection,
    behandlingService: BehandlingService
) : LagreBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(LagreInnvilgetKroniskSyktBarnRiver::class.java),
    behov = Behov.InnvilgetKroniskSyktBarn,
    behandlingType = BehandlingType.KRONISK_SYKT_BARN,
    behandlingStatus = BehandlingStatus.INNVILGET,
    behandlingService = behandlingService
)


internal class InitierAvslåttKroniskSyktBarnRiver(
    rapidsConnection: RapidsConnection
) : InitierBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(InitierAvslåttKroniskSyktBarnRiver::class.java),
    behov = Behov.AvslåttKroniskSyktBarn,
    identitetsnummerKeys = Behov.identitetsnummerKeys
)

internal class LagreAvslåttKroniskSyktBarnRiver(
    rapidsConnection: RapidsConnection,
    behandlingService: BehandlingService
) : LagreBehandlingRiver(
    rapidsConnection = rapidsConnection,
    logger = LoggerFactory.getLogger(LagreAvslåttKroniskSyktBarnRiver::class.java),
    behov = Behov.AvslåttKroniskSyktBarn,
    behandlingType = BehandlingType.KRONISK_SYKT_BARN,
    behandlingStatus = BehandlingStatus.AVSLÅTT,
    behandlingService = behandlingService
)