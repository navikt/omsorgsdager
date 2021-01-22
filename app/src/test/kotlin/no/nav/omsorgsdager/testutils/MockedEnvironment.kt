package no.nav.omsorgsdager.testutils

import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

internal class MockedEnvironment(
    wireMockPort: Int = 8082
) {

    internal val wireMockServer = WireMockBuilder()
        .withPort(wireMockPort)
        .build()

    internal val appConfig = mutableMapOf<String, String>()

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}