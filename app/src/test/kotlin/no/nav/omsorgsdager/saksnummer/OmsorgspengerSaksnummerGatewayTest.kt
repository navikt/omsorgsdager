package no.nav.omsorgsdager.saksnummer

import kotlinx.coroutines.runBlocking
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(ApplicationContextExtension::class)
internal class OmsorgspengerSaksnummerGatewayTest(
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


}