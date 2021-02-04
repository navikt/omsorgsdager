package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert

private val authorizationHeader = "Bearer ${Azure.V2_0.generateJwt("omsorgsdager", "omsorgsdager")}"

private fun TestApplicationCall.assertForventetResponse(forventetResponse: String?) {
    if (forventetResponse == null) {
        assertEquals(forventetResponse, response.content)
    } else {
        JSONAssert.assertEquals(forventetResponse, response.content, true)
    }
}

internal fun TestApplicationEngine.nyttVedtak(
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

internal fun TestApplicationEngine.aksjonspunkt(
    behandlingId: BehandlingId,
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kroniskt-sykt-barn/$behandlingId/aksjonspunkt") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(requestBody)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.innvilgelse(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kroniskt-sykt-barn/$behandlingId/innvilgelse") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.avslag(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kroniskt-sykt-barn/$behandlingId/avslag") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.forkast(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kroniskt-sykt-barn/$behandlingId/forkast") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.hentBehandling(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Get, "/api/kroniskt-sykt-barn?behandlingId=$behandlingId") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.hentSak(
    saksnummer: Saksnummer,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Get, "/api/kroniskt-sykt-barn?saksnummer=$saksnummer") {
        addHeader(HttpHeaders.Authorization, authorizationHeader)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}