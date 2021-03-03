package no.nav.omsorgsdager.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.person.PersonInfo
import org.json.JSONArray
import org.json.JSONObject

private const val pdlBasePath = "/pdl-mock"

private fun WireMockServer.mockPdlHentPersonInfo(correlationId: CorrelationId, response: ResponseDefinitionBuilder): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock
            .urlPathMatching(".*$pdlBasePath.*"))
            .withHeader("Authorization", WireMock.containing("Bearer e"))
            .withHeader("Content-Type", WireMock.equalTo("application/json"))
            .withHeader("TEMA", WireMock.equalTo("OMS"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("omsorgsdager"))
            .withHeader("Nav-Call-Id", WireMock.equalTo("$correlationId"))
            .willReturn(response)
    )
    return this
}

private fun WireMockServer.mockPdlPing() : WireMockServer {
    WireMock.stubFor(
        WireMock.options(WireMock
            .urlPathMatching(".*$pdlBasePath.*"))
            .withHeader("Authorization", WireMock.containing("Bearer e"))
            .willReturn(aResponse().withStatus(200))
    )
    return this
}

internal fun WireMockServer.mockPdlHentPersonInfo(
    personInfo: Map<AktørId, PersonInfo>,
    correlationId: CorrelationId,
    code: String = "ok") = mockPdlHentPersonInfo(
    correlationId = correlationId,
    response = aResponse().withStatus(200).withBody(personInfo.somPdlResponse(code))
)

private fun Map<AktørId, PersonInfo>.somPdlResponse(code: String) : String {
    val data = JSONObject()
    val hentPersonBolk = JSONArray()
    val hentIdenterBolk = JSONArray()
    forEach { aktørId, personInfo ->
        hentPersonBolk.put(JSONObject("""
        {
            "ident": "$aktørId",
            "person": {
                "foedsel": [{
                    "foedselsdato": "${personInfo.fødselsdato}"
                }]
            },
            "code": "$code"
        }
        """.trimIndent()))
        hentIdenterBolk.put(JSONObject("""
        {
            "ident": "$aktørId",
            "identer": [{
                "ident": "${personInfo.identitetsnummer}"
            }],
            "code": "$code"
        }
        """.trimIndent()))
    }
    data.put("hentPersonBolk", hentPersonBolk)
    data.put("hentIdenterBolk", hentIdenterBolk)
    return JSONObject().also { it.put("data", data) }.toString()
}

internal fun WireMockServer.stubPdl() = mockPdlPing()
internal fun WireMockServer.pdlBaseUrl() = baseUrl() + pdlBasePath
