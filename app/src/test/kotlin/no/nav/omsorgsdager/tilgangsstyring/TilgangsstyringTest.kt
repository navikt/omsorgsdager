package no.nav.omsorgsdager.tilgangsstyring

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.http.*
import io.ktor.server.plugins.callid.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TilgangsstyringTest {
    private val omsorgspengerTilgangsstyringGatewayMock = mockk<OmsorgspengerTilgangsstyringGateway>()

    private val tilgangsstyring = Tilgangsstyring(
        tokenResolver = TokenResolver(
            azureIssuers = setOf(Azure.V2_0.getIssuer()),
            openAmIssuers = setOf(NaisSts.getIssuer()),
            openAmAuthorizedClients = setOf("k9-sak")
        ),
        omsorgspengerTilgangsstyringGateway = omsorgspengerTilgangsstyringGatewayMock
    )

    @BeforeEach
    fun clear() {
        clearMocks(omsorgspengerTilgangsstyringGatewayMock)
    }


    @Test
    fun `Azure system token med tilgang`() {
        assertOperasjoner(
            jwt = azureSystemToken(medTilgang = true),
            forventetResultatVisning = true,
            forventetResultatEndring = true
        )
        tilgangsstyringGatewayKaltAkkurat(0)
    }

    @Test
    fun `Azure system token uten tilgang`() {
        assertOperasjoner(
            jwt = azureSystemToken(medTilgang = false),
            forventetResultatVisning = false,
            forventetResultatEndring = false
        )
        tilgangsstyringGatewayKaltAkkurat(0)
    }

    @Test
    fun `Azure person token som kun kan lese`() {
        val jwt = azurePersonToken()
        mockTilgangsstyringGateway(
            jwt = jwt,
            tilgangVisning = true,
            tilgangEndring = false
        )
        assertOperasjoner(
            jwt = jwt,
            forventetResultatVisning = true,
            forventetResultatEndring = false

        )
        tilgangsstyringGatewayKaltAkkurat(2)
    }

    @Test
    fun `Azure person token som kan lese og skrive`() {
        val jwt = azurePersonToken()
        mockTilgangsstyringGateway(
            jwt = jwt,
            tilgangVisning = true,
            tilgangEndring = true
        )
        assertOperasjoner(
            jwt = jwt,
            forventetResultatVisning = true,
            forventetResultatEndring = true

        )
        tilgangsstyringGatewayKaltAkkurat(2)
    }

    @Test
    fun `Azure person token uten tilgang`() {
        val jwt = azurePersonToken()
        mockTilgangsstyringGateway(
            jwt = jwt,
            tilgangVisning = false,
            tilgangEndring = false
        )
        assertOperasjoner(
            jwt = jwt,
            forventetResultatVisning = false,
            forventetResultatEndring = false
        )
        tilgangsstyringGatewayKaltAkkurat(2)
    }

    @Test
    fun `Open AM system token med tilgang`() {
        assertOperasjoner(
            jwt = openAmSytemToken(medTilgang = true),
            forventetResultatVisning = true,
            forventetResultatEndring = true
        )
        tilgangsstyringGatewayKaltAkkurat(0)
    }

    @Test
    fun `Open AM system token uten tilgang`() {
        assertOperasjoner(
            jwt = openAmSytemToken(medTilgang = false),
            forventetResultatVisning = false,
            forventetResultatEndring = false
        )
        tilgangsstyringGatewayKaltAkkurat(0)
    }

    @Test
    fun `Open AM person token som kun kan lese`() {
        val jwt = openAmPersonToken()
        mockTilgangsstyringGateway(
            jwt = jwt,
            tilgangVisning = true,
            tilgangEndring = false
        )
        assertOperasjoner(
            jwt = jwt,
            forventetResultatVisning = true,
            forventetResultatEndring = false
        )
        tilgangsstyringGatewayKaltAkkurat(2)
    }

    @Test
    fun `Open AM person token som kan lese og skrive`() {
        val jwt = openAmPersonToken()
        mockTilgangsstyringGateway(
            jwt = jwt,
            tilgangVisning = true,
            tilgangEndring = true
        )
        assertOperasjoner(
            jwt = jwt,
            forventetResultatVisning = true,
            forventetResultatEndring = true
        )
        tilgangsstyringGatewayKaltAkkurat(2)
    }

    @Test
    fun `Open AM person token som uten tilgang`() {
        val jwt = openAmPersonToken()
        mockTilgangsstyringGateway(
            jwt = jwt,
            tilgangVisning = false,
            tilgangEndring = false
        )
        assertOperasjoner(
            jwt = jwt,
            forventetResultatVisning = false,
            forventetResultatEndring = false
        )
        tilgangsstyringGatewayKaltAkkurat(2)
    }

    private fun assertOperasjoner(
        jwt: String?,
        forventetResultatVisning: Boolean,
        forventetResultatEndring: Boolean
    ) {
        val call = call(authorizationHeader = "Bearer $jwt")
        assertEquals(forventetResultatVisning, VisningOperasjon.kanGjøreOperasjon(call))
        assertEquals(forventetResultatEndring, EndringOperasjon.kanGjøreOperasjon(call))
    }

    private fun Operasjon.kanGjøreOperasjon(call: ApplicationCall) = this.let { operasjon ->
        runBlocking {
            kotlin.runCatching { tilgangsstyring.verifiserTilgang(call = call, operasjon = operasjon) }.isSuccess
        }
    }

    private fun tilgangsstyringGatewayKaltAkkurat(n: Int) {
        coVerify(exactly = n) { omsorgspengerTilgangsstyringGatewayMock.harTilgang(any(), any(), any()) }
    }

    private fun mockTilgangsstyringGateway(
        jwt: String,
        tilgangVisning: Boolean,
        tilgangEndring: Boolean
    ) {
        coEvery {
            omsorgspengerTilgangsstyringGatewayMock.harTilgang(
                token = match { it.jwt == jwt },
                operasjon = match { it.type == Operasjon.Type.Visning },
                correlationId = any()
            )
        }.returns(tilgangVisning)
        coEvery {
            omsorgspengerTilgangsstyringGatewayMock.harTilgang(
                token = match { it.jwt == jwt },
                operasjon = match { it.type == Operasjon.Type.Endring },
                correlationId = any()
            )
        }.returns(tilgangEndring)
    }

    internal companion object {
        private val VisningOperasjon = Operasjon(
            type = Operasjon.Type.Visning,
            beskrivelse = "Tester tilgangsstyring",
            identitetsnummer = setOf("12345678911".somIdentitetsnummer())
        )

        private val EndringOperasjon = Operasjon(
            type = Operasjon.Type.Endring,
            beskrivelse = "Tester tilgangsstyring",
            identitetsnummer = setOf("12345678911".somIdentitetsnummer())
        )

        internal fun azureSystemToken(medTilgang: Boolean) = Azure.V2_0.generateJwt(
            clientId = "any",
            audience = "omsorgsdager",
            accessAsApplication = medTilgang
        )

        internal fun azurePersonToken() = Azure.V2_0.generateJwt(
            clientId = "any",
            audience = "sjekkes-ved-sjekk-på-signatur",
            accessAsApplication = false,
            overridingClaims = mapOf(
                "oid" to "something",
                "preferred_username" to "user"
            )
        )

        internal fun openAmSytemToken(medTilgang: Boolean) = NaisSts.generateJwt(
            application = "any",
            overridingClaims = mapOf(
                "azp" to when (medTilgang) {
                    true -> "k9-sak"
                    false -> "k9-noe-annet"
                }
            )
        )

        internal fun openAmPersonToken() = NaisSts.generateJwt(
            application = "any",
            overridingClaims = mapOf(
                "azp" to "any",
                "tokenName" to "id_token"
            )
        )

        private fun call(authorizationHeader: String) = mockk<ApplicationCall>().also {
            every { it.request.headers }.returns(Headers.build {
                append(HttpHeaders.Authorization, authorizationHeader)
            })
            every { it.callId }.returns("${CorrelationId.genererCorrelationId()}")
        }
    }
}