package no.nav.omsorgsdager

import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgsdager.kronisksyktbarn.InitierAvslåttKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.InitierInnvilgetKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.LagreAvslåttKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.LagreInnvilgetKroniskSyktBarnRiver
import no.nav.omsorgsdager.midlertidigalene.InitierAvslåttMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.InitierInnvilgetMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.LagreAvslåttMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.LagreInnvilgetMidlertidigAleneRiver
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    // Kronisk sykt barn
    InitierInnvilgetKroniskSyktBarnRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    LagreInnvilgetKroniskSyktBarnRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    InitierAvslåttKroniskSyktBarnRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    LagreAvslåttKroniskSyktBarnRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    // Midlertidig alene
    InitierInnvilgetMidlertidigAleneRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    LagreInnvilgetMidlertidigAleneRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    InitierAvslåttMidlertidigAleneRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    LagreAvslåttMidlertidigAleneRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })

    register(RapidsStateListener(onStateChange = { state -> applicationContext.rapidsState = state }))
}

class RapidsStateListener(val onStateChange: (state: RapidsState) -> Unit): RapidsConnection.StatusListener {
    override fun onStartup(rapidsConnection: RapidsConnection) = onStateChange(rapidsConnection.state(RapidsStatus.STARTUP))
    override fun onShutdown(rapidsConnection: RapidsConnection) = onStateChange(rapidsConnection.state(RapidsStatus.SHUTDOWN))
    override fun onNotReady(rapidsConnection: RapidsConnection) = onStateChange(rapidsConnection.state(RapidsStatus.NOT_READY))
    override fun onReady(rapidsConnection: RapidsConnection) = onStateChange(rapidsConnection.state(RapidsStatus.READY))

    private fun RapidsConnection.rapidsConnectionIsReady() = when (this)  {
        is KafkaRapid -> this.isReady()
        else -> true
    }

    private fun RapidsConnection.state(status: RapidsStatus) = RapidsState(
        stateAt = ZonedDateTime.now(),
        isReady = this.rapidsConnectionIsReady(),
        status = status
    )

    data class RapidsState(
        val stateAt: ZonedDateTime,
        val status: RapidsStatus,
        val isReady: Boolean) {
        val asMap = mapOf(
            "stateAt" to "$stateAt",
            "status" to status.name,
            "isReady" to isReady
        )
        fun isHealthy() = when (status) {
            RapidsStatus.READY -> isReady
            RapidsStatus.SHUTDOWN -> statusVedvartMindreEnn5Minutter()
            RapidsStatus.STARTUP -> statusVedvartMindreEnn5Minutter()
            RapidsStatus.NOT_READY -> false
        }

        private fun statusVedvartMindreEnn5Minutter() =
            ChronoUnit.MINUTES.between(stateAt, ZonedDateTime.now()) < 5L

        companion object {
            fun initialState() = RapidsState(
                stateAt = ZonedDateTime.now(),
                status = RapidsStatus.STARTUP,
                isReady = false
            )
        }
    }

    enum class RapidsStatus {
        STARTUP,
        SHUTDOWN,
        NOT_READY,
        READY
    }
}