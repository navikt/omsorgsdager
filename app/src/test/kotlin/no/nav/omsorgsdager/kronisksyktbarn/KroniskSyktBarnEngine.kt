package no.nav.omsorgsdager.kronisksyktbarn

import com.github.tomakehurst.wiremock.http.Cookie
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.tilgangsstyring.TilgangsstyringTest.Companion.azureSystemToken
import no.nav.omsorgsdager.tilgangsstyring.TilgangsstyringTest.Companion.openAmPersonToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZonedDateTime

private val authorizationHeaderSystem = "Bearer ${azureSystemToken(medTilgang = true)}"
private val cookieSaksbehandler = Cookie(listOf("ID_token=${openAmPersonToken()}", "Path=/", "Domain=localhost")).toString()

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
    handleRequest(HttpMethod.Post, "/api/kronisk-sykt-barn") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(requestBody)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.løs(
    behandlingId: BehandlingId,
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kronisk-sykt-barn/$behandlingId/løst") {
        addHeader(HttpHeaders.Cookie, cookieSaksbehandler)
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
    handleRequest(HttpMethod.Patch, "/api/kronisk-sykt-barn/$behandlingId/innvilget") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.avslag(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kronisk-sykt-barn/$behandlingId/avslått") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody("""{"tidspunkt":"${ZonedDateTime.now()}"}""")
    }.apply {
        assertEquals(forventetStatusCode, response.status())

        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.forkast(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Patch, "/api/kronisk-sykt-barn/$behandlingId/forkastet") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.hentBehandling(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Get, "/api/kronisk-sykt-barn?behandlingId=$behandlingId") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
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
    handleRequest(HttpMethod.Get, "/api/kronisk-sykt-barn?saksnummer=$saksnummer") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}