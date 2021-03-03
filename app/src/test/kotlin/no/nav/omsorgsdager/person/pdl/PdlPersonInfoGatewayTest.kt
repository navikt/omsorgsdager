package no.nav.omsorgsdager.person.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.person.PersonInfo
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.wiremock.mockPdlHentPersonInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(ApplicationContextExtension::class)
internal class PdlPersonInfoGatewayTest(
    private val wireMockServer: WireMockServer,
    applicationContextBuilder: ApplicationContext.Builder) {

    private val client = applicationContextBuilder.build().personInfoGatway as PdlPersonInfoGateway

    @Test
    fun `Hente personinfo for alle aktørIder`() {
        val correlationId = CorrelationId.genererCorrelationId()
        val aktørId1 = "12345".somAktørId()
        val aktørId2 = "56789".somAktørId()
        val forventet = mapOf(
            aktørId1 to PersonInfo(
                fødselsdato = LocalDate.now(),
                identitetsnummer = "11111111111".somIdentitetsnummer()
            ),
            aktørId2 to PersonInfo(
                fødselsdato = LocalDate.now(),
                identitetsnummer = "11111111112".somIdentitetsnummer()
            )
        )

        wireMockServer.mockPdlHentPersonInfo(personInfo = forventet, correlationId = correlationId)

        val resultat = hent(aktørIder = setOf(aktørId1, aktørId2), correlationId = correlationId)

        assertEquals(forventet, resultat)
    }

    @Test
    fun `Mangler personinfo for en aktørId`() {
        val correlationId = CorrelationId.genererCorrelationId()
        val aktørId1 = "12345".somAktørId()
        val aktørId2 = "56789".somAktørId()

        val pdlPersonInfo = mapOf(
            aktørId2 to PersonInfo(
                fødselsdato = LocalDate.now(),
                identitetsnummer = "11111111112".somIdentitetsnummer()
            )
        )

        wireMockServer.mockPdlHentPersonInfo(personInfo = pdlPersonInfo, correlationId = correlationId)

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            hent(aktørIder = setOf(aktørId1, aktørId2), correlationId = correlationId)
        }

    }

    @Test
    fun `Annet enn ok code fra PDL`() {
        val correlationId = CorrelationId.genererCorrelationId()
        val aktørId1 = "12345".somAktørId()
        val aktørId2 = "56789".somAktørId()
        val forventet = mapOf(
            aktørId1 to PersonInfo(
                fødselsdato = LocalDate.now(),
                identitetsnummer = "11111111111".somIdentitetsnummer()
            ),
            aktørId2 to PersonInfo(
                fødselsdato = LocalDate.now(),
                identitetsnummer = "11111111112".somIdentitetsnummer()
            )
        )

        wireMockServer.mockPdlHentPersonInfo(personInfo = forventet, correlationId = correlationId, code = "error")

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            hent(aktørIder = setOf(aktørId1, aktørId2), correlationId = correlationId)
        }
    }

    @Test
    fun `Ping check`() {
        assertTrue(runBlocking{ client.pingCheck()} is Healthy)
    }

    private fun hent(aktørIder: Set<AktørId>, correlationId: CorrelationId) = runBlocking {
        client.hent(aktørIder, correlationId)
    }
}