package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val basePath = "/omsorgspenger-rammemeldinger-mock"


private fun WireMockServer.stubIsReady(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$basePath.*/isready"))
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

internal fun WireMockServer.stubOmsorgspengerRammemeldinger() =
    stubIsReady()
internal fun WireMockServer.omsorgspengerRammemeldingerBaseUrl() = baseUrl() + basePath
