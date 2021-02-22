package no.nav.omsorgsdager

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgsdager.kronisksyktbarn.InitierAvslåttKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.InitierInnvilgetKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.LagreAvslåttKroniskSyktBarnRiver
import no.nav.omsorgsdager.kronisksyktbarn.LagreInnvilgetKroniskSyktBarnRiver
import no.nav.omsorgsdager.midlertidigalene.InitierAvslåttMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.InitierInnvilgetMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.LagreAvslåttMidlertidigAleneRiver
import no.nav.omsorgsdager.midlertidigalene.LagreInnvilgetMidlertidigAleneRiver

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .withKtorModule { omsorgsdager(applicationContext) }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    // Kronisk sykt barn
    InitierInnvilgetKroniskSyktBarnRiver(rapidsConnection = this)
    LagreInnvilgetKroniskSyktBarnRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    InitierAvslåttKroniskSyktBarnRiver(rapidsConnection = this)
    LagreAvslåttKroniskSyktBarnRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    // Midlertidig alene
    InitierInnvilgetMidlertidigAleneRiver(rapidsConnection = this)
    LagreInnvilgetMidlertidigAleneRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)
    InitierAvslåttMidlertidigAleneRiver(rapidsConnection = this)
    LagreAvslåttMidlertidigAleneRiver(rapidsConnection = this, behandlingService = applicationContext.behandlingService)

    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })
}