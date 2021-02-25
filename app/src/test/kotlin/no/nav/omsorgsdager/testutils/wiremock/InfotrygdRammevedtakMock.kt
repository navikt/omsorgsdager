package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val apiPath = "/"

private fun WireMockServer.stubDefaultTomtSvar(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock.urlPathMatching(".*$apiPath.*/rammevedtak"))
            .atPriority(catchAllPriority)
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.personIdent", AnythingPattern()))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "rammevedtak": {} }""")
            )
    )
    return this
}

private fun WireMockServer.stubUtvidetOchMidlAlene(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock.urlPathMatching(".*$apiPath.*/rammevedtak"))
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.personIdent", equalTo("29099022222")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                           "rammevedtak": {
                           "UtvidetRett": [{
                                "vedtatt": "2020-06-21",
                                "barn": {
                                    "id": "01019911111",
                                    "type": "PersonIdent",
                                    "fødselsdato": "1999-01-01"
                                },
                                "gyldigFraOgMed": "2020-06-21",
                                "gyldigTilOgMed": "2020-06-21",
                                "lengde": "PT480H",
                                "kilder": [{
                                    "id": "UTV.RETT/20D/29099022222",
                                    "type": "Personkort"
                                }]
                            }],
                            "MidlertidigAleneOmOmsorgen": [{
                                "vedtatt": "1998-06-21",
                                "kilder": [{
                                    "id": "midl.alene.om/17D",
                                    "type": "Personkort"
                                }],
                                "gyldigFraOgMed": "1998-06-25",
                                "gyldigTilOgMed": "2001-06-25",
                                "lengde": "PT408H"
                            }]
                           }
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

internal fun WireMockServer.stubInfotrygdRammevedtak() = stubInfotrygdRammevedtakHelsesjekk()
    .stubDefaultTomtSvar()
    .stubUtvidetOchMidlAlene()
internal fun WireMockServer.infotrygdRammevedtakBaseUrl() = baseUrl() + apiPath
