package no.nav.omsorgsdager.person.pdl

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpOptions
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgsdager.AzureAwareGateway
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.Json.Companion.somJsonOrNull
import no.nav.omsorgsdager.SecureLogger
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.person.PersonInfo
import no.nav.omsorgsdager.person.PersonInfoGateway
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

internal class PdlPersonInfoGateway(
    baseUri: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : PersonInfoGateway, AzureAwareGateway(
    navn = "PdlPersonInfoGateway",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUri = URI("$baseUri/graphql")
) {
    private val graphqlEndpoint = "$baseUri/graphql"

    override suspend fun hent(aktørIder: Set<AktørId>, correlationId: CorrelationId): Map<AktørId, PersonInfo> {
        val pdlRequest = hentPersonInfoRequest(aktørIder)

        val (httpStatusCode, response) = graphqlEndpoint.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerId.first, ConsumerId.second)
            it.header(Tema.first, Tema.second)
            it.header(OmsorgspengerRammemelding.first, OmsorgspengerRammemelding.second)
            it.accept(ContentType.Application.Json)
            it.jsonBody(pdlRequest)
        }.readTextOrThrow()

        return kotlin.runCatching {
            require(HttpStatusCode.OK == httpStatusCode) {
                "Uventet $httpStatusCode fra PDL."
            }

            val data = JSONObject(response).getJSONObject("data")
            val hentPersonBolk = data.getJSONArray("hentPersonBolk").also { it.inneholderKunOkCodes() }
            val hentIdenterBolk = data.getJSONArray("hentIdenterBolk").also { it.inneholderKunOkCodes() }

            aktørIder.associateWith { aktørId ->
                PersonInfo(
                    identitetsnummer = (hentIdenterBolk.tilhørende(aktørId).getJSONArray("identer")
                        .first() as JSONObject).getString("ident").somIdentitetsnummer(),
                    fødselsdato = (hentPersonBolk.tilhørende(aktørId).getJSONObject("person").getJSONArray("foedsel")
                        .first() as JSONObject).getString("foedselsdato").dato()
                )
            }
        }.fold(
            onSuccess = { it },
            onFailure = {
                SecureLogger.error("PdlRequest=$pdlRequest, PdlResponse=${response.somJsonOrNull() ?: response}")
                throw IllegalStateException("Feil ved oppslag mot PDL. Se sikker log former detaljer.", it)
            }
        )
    }

    override suspend fun pingCheck(): Result {
        return pingUri.toString().httpOptions {
            it.header(HttpHeaders.Authorization, authorizationHeader())
        }.second.fold(
            onSuccess = { Healthy("PingCheck", "OK!") },
            onFailure = { UnHealthy("PingCheck", "Feil: ${it.message}") }
        )
    }

    internal companion object {
        private const val CorrelationIdHeaderKey = "Nav-Call-Id"
        private val ConsumerId = "Nav-Consumer-Id" to "omsorgsdager"
        private val Tema = "TEMA" to "OMS"
            // https://behandlingskatalog.intern.nav.no/process/purpose/PLEIE_OMSORGS_OG_OPPLAERINGSPENGER/4a1c9324-9c5e-4ddb-ac7f-c55d1dcd9736
        private const val OmsorgspengerRammemelding = "Behandlingsnummer" to "B142"

        private val Query = """
        query(${"$"}identer: [ID!]!) {
            hentPersonBolk(identer: ${"$"}identer) {
                ident,
                person {
                    foedsel {
                        foedselsdato
                    }
                },
                code
            },
            hentIdenterBolk(identer: ${"$"}identer, grupper: [FOLKEREGISTERIDENT], historikk: false) {
                ident,
                identer {
                    ident
                },
                code
            }
        }
        """.trimIndent().replace("\n", "").replace("  ", "")

        internal fun hentPersonInfoRequest(aktørIder: Set<AktørId>): String {
            val request = JSONObject()
            request.put("query", Query)
            request.put("variables", JSONObject().also { it.put("identer", aktørIder.map { "$it" }) })
            return request.toString()
        }

        private fun JSONArray.inneholderKunOkCodes() = require(all { (it as JSONObject).getString("code") == "ok" }) {
            "Response fra PDL inneholder ikke bare ok codes."
        }

        private fun JSONArray.tilhørende(aktørId: AktørId) =
            first { (it as JSONObject).getString("ident") == "$aktørId" } as JSONObject
    }

}
