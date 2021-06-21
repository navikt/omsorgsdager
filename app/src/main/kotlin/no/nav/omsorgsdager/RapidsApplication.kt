package no.nav.omsorgsdager

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakRiver

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {

    // Kronisk sykt barn
    no.nav.omsorgsdager.kronisksyktbarn.InitierInnvilgetKroniskSyktBarnRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    no.nav.omsorgsdager.kronisksyktbarn.LagreInnvilgetKroniskSyktBarnRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    no.nav.omsorgsdager.kronisksyktbarn.InitierAvslåttKroniskSyktBarnRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    no.nav.omsorgsdager.kronisksyktbarn.LagreAvslåttKroniskSyktBarnRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    // Midlertidig alene
    no.nav.omsorgsdager.midlertidigalene.InitierInnvilgetMidlertidigAleneRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    no.nav.omsorgsdager.midlertidigalene.LagreInnvilgetMidlertidigAleneRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    no.nav.omsorgsdager.midlertidigalene.InitierAvslåttMidlertidigAleneRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    no.nav.omsorgsdager.midlertidigalene.LagreAvslåttMidlertidigAleneRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    // Alene omsorg
    no.nav.omsorgsdager.aleneomsorg.InitierInnvilgetAleneOmsorgRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    no.nav.omsorgsdager.aleneomsorg.LagreInnvilgetAleneOmsorgRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    no.nav.omsorgsdager.aleneomsorg.InitierAvslåttAleneOmsorgRiver(rapidsConnection = this, personInfoGateway = applicationContext.personInfoGatway)
    no.nav.omsorgsdager.aleneomsorg.LagreAvslåttAleneOmsorgRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    // Hente Innvilgede Vedtak
    InnvilgedeVedtakRiver(rapidsConnection = this, innvilgedeVedtakService = applicationContext.innvilgedeVedtakService)

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