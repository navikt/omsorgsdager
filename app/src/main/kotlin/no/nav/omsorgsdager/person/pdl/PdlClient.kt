package no.nav.omsorgsdager.person.pdl

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.person.PersonInfo
import no.nav.omsorgsdager.person.PersonInfoGateway
import java.net.URI

internal class PdlPersonInfoGateway(
    baseUri: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : PersonInfoGateway, AzureAwareGateway(
    navn = "PdlPersonInfoGateway",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUri = URI("$baseUri/TODO"),
    pingRequest = {  Fuel.request(Method.OPTIONS, it.toString()) }) {

    override suspend fun hent(aktørIder: Set<AktørId>): Map<AktørId, PersonInfo> {
        val result = mapOf<AktørId, PersonInfo>()
        require(result.keys == aktørIder) {
            "Fant ikke alle aktørene ved oppslag mot PDL"
        }
        return result
    }

    private companion object {
        val query = PdlPersonInfoGateway::class.java.getResource("/pdl/hentPersonInfo.graphql").readText().replace("[\n\r]", "")
    }
}