package no.nav.omsorgsdager.utvidetrett

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.BehandlingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert

private val authorizationHeader = "Bearer ${Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager")}"

private fun TestApplicationCall.assertForventetResponse(forventetResponse: String?) {
    if (forventetResponse == null) {
        assertEquals(forventetResponse, response.content)
    } else {
        JSONAssert.assertEquals(forventetResponse, forventetResponse, true)
    }
}

internal fun TestApplicationEngine.nySøknad(
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.Created,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Post, "/api/kroniskt-sykt-barn") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(requestBody)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.aksjonspunkter(
    behandlingId: BehandlingId,
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Put, "/api/kroniskt-sykt-barn/$behandlingId/aksjonspunkt") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(requestBody)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.fastsett(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Put, "/api/kroniskt-sykt-barn/$behandlingId/fastsett") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.deaktiver(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Put, "/api/kroniskt-sykt-barn/$behandlingId/deaktiver") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}