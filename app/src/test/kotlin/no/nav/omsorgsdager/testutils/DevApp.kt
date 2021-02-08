package no.nav.omsorgsdager.testutils

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.testutils.MockedEnvironment.Companion.omsorgsdager
import org.slf4j.LoggerFactory

internal class DevApp(applicationContext: ApplicationContext) {
    companion object {
        private val logger = LoggerFactory.getLogger(DevApp::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val mockedEnvironment = MockedEnvironment(applicationPort = 8081).start()
            val devApp = DevApp(mockedEnvironment.applicationContext)

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    devApp.stop()
                    mockedEnvironment.stop()
                    logger.info("Tear down complete")
                }
            })

            devApp.start()
        }
    }

    private val server = embeddedServer(Netty, applicationEngineEnvironment {
        omsorgsdager(applicationContext)
    })

    private fun start() = server.start(wait = true)
    private fun stop() = server.stop(10000, 10000)
}