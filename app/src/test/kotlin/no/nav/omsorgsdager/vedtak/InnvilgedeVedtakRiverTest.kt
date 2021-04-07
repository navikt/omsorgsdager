package no.nav.omsorgsdager.vedtak

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgsdager.*
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.testutils.sisteMeldingHarLøsningPå
import no.nav.omsorgsdager.testutils.sisteMeldingSomJSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class InnvilgedeVedtakRiverTest(
    applicationContextBuilder: ApplicationContext.Builder) : InnvilgedeVedtakKontrakt(applicationContextBuilder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(applicationContext)
    }

    override fun hentInnvilgedeVedtak(jsonRequest: Json): Json {
        val behovssekvens = behovssekvens(jsonRequest)
        rapid.sendTestMessage(behovssekvens)
        rapid.sisteMeldingHarLøsningPå(Behov)
        return rapid.løsningJson()
    }

    @Test
    fun `ugyldig behov`() {
        val behovssekvens = behovssekvens(Json.tomJson())
        rapid.sendTestMessage(behovssekvens)
        assertEquals(0, rapid.inspektør.size)
    }

    private companion object {
        const val Behov = "HentUtvidetRettVedtakV2"
        fun behovssekvens(
            jsonRequest: Json
        ) = Behovssekvens(
            id = "${BehovssekvensId.genererBehovssekvensId()}",
            correlationId = "${CorrelationId.genererCorrelationId()}",
            behov = arrayOf(Behov(navn = Behov, input = jsonRequest.map))
        ).keyValue.second

        fun TestRapid.løsningJson() = sisteMeldingSomJSONObject()
            .getJSONObject("@løsninger")
            .getJSONObject(Behov)
            .also { it.remove("løst") }.somJson()
    }
}