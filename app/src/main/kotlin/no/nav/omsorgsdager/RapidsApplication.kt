package no.nav.omsorgsdager

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.omsorgsdager.kronisksyktbarn.InitierAvslåttKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.InitierInnvilgetKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.LagreAvslåttKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.LagreInnvilgetKroniskSyktBarnRiver
import no.nav.omsorgsdager.midlertidigalene.InitierAvslåttMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.InitierInnvilgetMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.LagreAvslåttMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.LagreInnvilgetMidlertidigAleneRiver

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