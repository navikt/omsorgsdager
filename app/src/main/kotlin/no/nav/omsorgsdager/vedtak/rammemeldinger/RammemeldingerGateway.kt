package no.nav.omsorgsdager.vedtak.rammemeldinger

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.vedtak.dto.AleneOmsorgInnvilgetVedtak
import java.net.URI

internal class RammemeldingerGateway(
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>,
    baseUrl: URI
) : AzureAwareGateway(
    navn = "RammemeldingerGateway",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUri = URI("$baseUrl/isready")) {

    internal suspend fun hentAleneOmsorg(
        saksnummer: OmsorgspengerSaksnummer,
        correlationId: CorrelationId) : List<AleneOmsorgInnvilgetVedtak> {
        return emptyList()
    }
}