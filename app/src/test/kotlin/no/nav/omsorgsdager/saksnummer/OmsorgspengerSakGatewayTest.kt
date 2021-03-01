package no.nav.omsorgsdager.saksnummer

import kotlinx.coroutines.runBlocking
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class OmsorgspengerSakGatewayTest(
    applicationContextBuilder: ApplicationContext.Builder) {

    private val applicationContext = applicationContextBuilder.build()
    private val omsorgspengerSakGateway = applicationContext.omsorgspengerSakGateway

    @Test
    fun `Person som har saksnummer i omsorgspenger-sak`() {
        val personIdentSaksnummer = "11111111111"
        val expectedResponse = "a1b2c3".somOmsorgspengerSaksnummer()

        val response = runBlocking {
            omsorgspengerSakGateway.hentSaksnummer(
            identitetsnummer = personIdentSaksnummer.somIdentitetsnummer(),
            correlationId = CorrelationId.genererCorrelationId())
        }

        assertEquals(expectedResponse, response)
    }

    @Test
    fun `Person som ikke har saksnummer i omsorgspenger-sak`() {
        val personIdentSaksnummer = "22222222222"

        val response = runBlocking {
            omsorgspengerSakGateway.hentSaksnummer(
                identitetsnummer = personIdentSaksnummer.somIdentitetsnummer(),
                correlationId = CorrelationId.genererCorrelationId())
        }

        assertNull(response)
    }

    @Test
    fun `Uventet response`() {
        val personIdentSaksnummer = "11111111112"

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            runBlocking {
                omsorgspengerSakGateway.hentSaksnummer(
                    identitetsnummer = personIdentSaksnummer.somIdentitetsnummer(),
                    correlationId = CorrelationId.genererCorrelationId())
            }
        }
    }
}