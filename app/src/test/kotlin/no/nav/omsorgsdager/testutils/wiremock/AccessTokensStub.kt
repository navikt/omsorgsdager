package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.ktor.http.*
import no.nav.omsorgsdager.tilgangsstyring.TilgangsstyringTest.Companion.azurePersonToken
import no.nav.omsorgsdager.tilgangsstyring.TilgangsstyringTest.Companion.azureSystemToken
import no.nav.omsorgsdager.tilgangsstyring.TilgangsstyringTest.Companion.openAmPersonToken
import no.nav.omsorgsdager.tilgangsstyring.TilgangsstyringTest.Companion.openAmSytemToken

private const val accessTokenPath = "/access-tokens"

internal fun WireMockServer.stubAccessTokens(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock
            .urlPathMatching(".*$accessTokenPath"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.ContentType, "application/json")
                    .withBody("""
                        {
                            "azure": {
                                "system": "${azureSystemToken(medTilgang = true)}",
                                "person": "${azurePersonToken()}"
                            },
                            "open-am": {
                                "system": "${openAmSytemToken(medTilgang = true)}",
                                "person": "${openAmPersonToken()}"
                            }
                        }
                    """.trimIndent())
            )
    )

    return this
}
