package no.nav.omsorgsdager.kronisksyktbarn

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.vedtak.*
import no.nav.omsorgsdager.vedtak.avslag
import no.nav.omsorgsdager.vedtak.innvilgelse
import no.nav.omsorgsdager.vedtak.løsBehov
import no.nav.omsorgsdager.vedtak.nyttVedtak

private const val Vedtak = "kronisk-sykt-barn"

internal fun TestApplicationEngine.nyttKroniskSyktBarnVedtak(
    requestBody: String,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.Created,
    forventetResponse: String? = null) = nyttVedtak(
        vedtak = Vedtak,
        requestBody = requestBody,
        forventetStatusCode = forventetStatusCode,
        forventetResponse = forventetResponse
    )

internal fun TestApplicationEngine.løsBehovKroniskSyktBarn(
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

internal fun TestApplicationEngine.innvilgelseKroniskSyktBarn(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = innvilgelse(
        vedtak = Vedtak,
        behandlingId = behandlingId,
        forventetStatusCode = forventetStatusCode,
        forventetResponse = forventetResponse
    )

internal fun TestApplicationEngine.avslagKroniskSyktBarn(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = avslag(
        vedtak = Vedtak,
        behandlingId = behandlingId,
        forventetStatusCode = forventetStatusCode,
        forventetResponse = forventetResponse
    )

internal fun TestApplicationEngine.forkastKroniskSyktBarn(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = forkast(
        vedtak = Vedtak,
        behandlingId = behandlingId,
        forventetStatusCode = forventetStatusCode,
        forventetResponse = forventetResponse
    )

internal fun TestApplicationEngine.hentBehandlingKroniskSyktBarn(
    behandlingId: BehandlingId,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = hentBehandling(
        vedtak = Vedtak,
        behandlingId = behandlingId,
        forventetStatusCode = forventetStatusCode,
        forventetResponse = forventetResponse
    )

internal fun TestApplicationEngine.hentSakKroniskSyktBarn(
    saksnummer: Saksnummer,
    forventetStatusCode: HttpStatusCode = HttpStatusCode.OK,
    forventetResponse: String? = null) = hentSak(
        vedtak = Vedtak,
        saksnummer = saksnummer,
        forventetStatusCode = forventetStatusCode,
        forventetResponse = forventetResponse
    )