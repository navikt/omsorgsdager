package no.nav.omsorgsdager.midlertidigalene

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.vedtak.*

private const val Vedtak = "midlertidig-alene"

internal fun TestApplicationEngine.nyttMidlertidigAleneVedtak(
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.Created,
    forventetResponse: String? = null) = nyttVedtak(
    vedtak = Vedtak,
    requestBody = requestBody,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)

internal fun TestApplicationEngine.løsBehovMidlertidigAlene(
    behandlingId: BehandlingId,
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = løsBehov(
    vedtak = Vedtak,
    behandlingId = behandlingId,
    requestBody = requestBody,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)

internal fun TestApplicationEngine.innvilgelseMidlertidigAlene(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = innvilgelse(
    vedtak = Vedtak,
    behandlingId = behandlingId,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)

internal fun TestApplicationEngine.avslagMidlertidigAlene(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = avslag(
    vedtak = Vedtak,
    behandlingId = behandlingId,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)

internal fun TestApplicationEngine.forkastMidlertidigAlene(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = forkast(
    vedtak = Vedtak,
    behandlingId = behandlingId,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)

internal fun TestApplicationEngine.hentBehandlingMidlertidigAlene(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = hentBehandling(
    vedtak = Vedtak,
    behandlingId = behandlingId,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)

internal fun TestApplicationEngine.hentSakMidlertidigAlene(
    saksnummer: Saksnummer,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = hentSak(
    vedtak = Vedtak,
    saksnummer = saksnummer,
    forventetStatusCode = forventetStatusCode,
    forventetResponse = forventetResponse
)