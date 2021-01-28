package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing

private const val pdlApiBasePath = "/pdlapi-mock"
private const val pdlApiMockPath = "/"

private fun WireMockServer.stubPdlApiHealthCheck(): WireMockServer {
    WireMock.stubFor(
            WireMock.options(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                    )
    )

    return this
}

internal fun WireMockServer.stubPdlApi() = stubPdlApiHealthCheck()

internal fun WireMockServer.pdlApiBaseUrl() = baseUrl() + pdlApiBasePath
