package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import io.ktor.http.*

private const val apiPath = "/omsorgspenger-sak-mock"

private fun WireMockServer.stubDefaultTomtSvar(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock.urlPathMatching(".*$apiPath.*/saksnummer"))
            .atPriority(catchAllPriority)
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.identitetsnummer", AnythingPattern()))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatusCode.NotFound.value)
            )
    )
    return this
}

private fun WireMockServer.stubPersonSomHarSaksnummer(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock.urlPathMatching(".*$apiPath.*/saksnummer"))
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.identitetsnummer", equalTo("11111111111")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                           "saksnummer": "a1b2c3"
                        }
                        """.trimIndent()
                    )
            )
    )
    return this
}

private fun WireMockServer.stubInfotrygdRammevedtakHelsesjekk(): WireMockServer {
    WireMock.stubFor(
        WireMock.any(WireMock.urlPathMatching(".*$apiPath.*/isalive"))
            .willReturn(aResponse().withStatus(200)))
    WireMock.stubFor(
        WireMock.any(WireMock.urlPathMatching(".*$apiPath.*/isready"))
            .willReturn(aResponse().withStatus(200)))
    return this
}

internal fun WireMockServer.stubOmsorgspengerSakApi() = stubInfotrygdRammevedtakHelsesjekk()
    .stubDefaultTomtSvar()
    .stubPersonSomHarSaksnummer()
internal fun WireMockServer.omsorgspengerSakBaseUrl() = baseUrl() + apiPath
