package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import org.intellij.lang.annotations.Language

private const val basePath = "/omsorgspenger-rammemeldinger-mock"

@Language("JSON")
private val responseMed = """
{
      "aleneOmOmsorgen": [{
        "registrert": "2020-11-24T17:34:31.227Z",
        "gjelderFraOgMed": "2020-01-01",
        "gjelderTilOgMed": "2025-05-05",
        "barn": {
          "identitetsnummer": "12345678991",
          "fødselsdato": "2006-05-01"
        },
        "kilde": {
          "id":"foo",
          "type":"OmsorgspengerRammemeldinger[bar]"
        }
      },{
        "registrert": "2020-11-24T18:34:31.227Z",
        "gjelderFraOgMed": "2025-03-03",
        "gjelderTilOgMed": "2030-12-31",
        "barn": {
          "identitetsnummer": "12345678991",
          "fødselsdato": "2006-05-01"
        },
        "kilde": {
          "id":"foo2",
          "type":"OmsorgspengerRammemeldinger[bar2]"
        }
      }]
    }
""".trimIndent()

@Language("JSON")
private val responseUten = """
    {
      "aleneOmOmsorgen": []
    }
""".trimIndent()

private fun WireMockServer.stubMedAleneOmsorg() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$basePath.*/alene-om-omsorgen"))
            .withQueryParam("saksnummer", equalTo("SAKMED")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("content-type", "application/json")
                    .withBody(responseMed)
            )
    )
    return this
}

private fun WireMockServer.stubUtenAleneOmsorg() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$basePath.*/alene-om-omsorgen"))
            .withQueryParam("saksnummer", equalTo("SAKUTEN")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("content-type", "application/json")
                    .withBody(responseUten)
            )
    )
    return this
}

private fun WireMockServer.stubIsReady(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$basePath.*/isready"))
            .willReturn(aResponse().withStatus(200)))
    return this
}

internal fun WireMockServer.stubOmsorgspengerRammemeldinger() =
    stubIsReady()
    .stubUtenAleneOmsorg()
    .stubMedAleneOmsorg()

internal fun WireMockServer.omsorgspengerRammemeldingerBaseUrl() = baseUrl() + basePath
