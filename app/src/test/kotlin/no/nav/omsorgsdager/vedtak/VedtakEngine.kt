package no.nav.omsorgsdager.vedtak

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
    vedtak: String,
    requestBody: String,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String?) {
    handleRequest(HttpMethod.Post, "/api/$vedtak") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(requestBody)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.løsBehov(
    vedtak: String,
    behandlingId: BehandlingId,
    requestBody: String,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String?) {
    handleRequest(HttpMethod.Patch, "/api/$vedtak/$behandlingId/løst") {
        addHeader(HttpHeaders.Cookie, cookieSaksbehandler)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(requestBody)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.innvilgelse(
    vedtak: String,
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String?) {
    handleRequest(HttpMethod.Patch, "/api/$vedtak/$behandlingId/innvilget") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.avslag(
    vedtak: String,
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String?) {
    handleRequest(HttpMethod.Patch, "/api/$vedtak/$behandlingId/avslått") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody("""{"tidspunkt":"${ZonedDateTime.now()}"}""")
    }.apply {
        assertEquals(forventetStatusCode, response.status())

        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.forkast(
    vedtak: String,
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String?) {
    handleRequest(HttpMethod.Patch, "/api/$vedtak/$behandlingId/forkastet") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.hentBehandling(
    vedtak: String,
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Get, "/api/$vedtak?behandlingId=$behandlingId") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        println(response.content)
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}

internal fun TestApplicationEngine.hentSak(
    vedtak: String,
    saksnummer: Saksnummer,
    forventetStatusCode: HttpStatusCode,
    forventetResponse: String? = null) {
    handleRequest(HttpMethod.Get, "/api/$vedtak?saksnummer=$saksnummer") {
        addHeader(HttpHeaders.Authorization, authorizationHeaderSystem)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }.apply {
        assertEquals(forventetStatusCode, response.status())
        assertForventetResponse(forventetResponse)
    }
}