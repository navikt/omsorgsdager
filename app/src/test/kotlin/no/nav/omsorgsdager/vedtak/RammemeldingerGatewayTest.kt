package no.nav.omsorgsdager.vedtak

import kotlinx.coroutines.runBlocking
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer.Companion.somOmsorgspengerSaksnummer
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.tid.Periode.Companion.dato
import no.nav.omsorgsdager.tid.Periode.Companion.periode
import no.nav.omsorgsdager.tid.Periode.Companion.tidspunkt
import no.nav.omsorgsdager.vedtak.dto.AleneOmsorgInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.Kilde
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class RammemeldingerGatewayTest(
    applicationContextBuilder: ApplicationContext.Builder) {
    private val applicationContext = applicationContextBuilder.build()
    private val rammemeldingerGateway = applicationContext.rammemeldingerGateway

    @Test
    fun `Person med alene om omsorgen registrert som rammemelding`() {

        val forventet = listOf(
            AleneOmsorgInnvilgetVedtak(
                tidspunkt = "2020-11-24T17:34:31.227Z".tidspunkt(),
                periode = "2020-01-01/2025-05-05".periode(),
                barn = Barn(identitetsnummer = "12345678991".somIdentitetsnummer(), fødselsdato = "2006-05-01".dato()),
                kilder = setOf(Kilde(id = "foo", type = "OmsorgspengerRammemeldinger[bar]"))
            ),
            AleneOmsorgInnvilgetVedtak(
                tidspunkt = "2020-11-24T18:34:31.227Z".tidspunkt(),
                periode = "2025-03-03/2030-12-31".periode(),
                barn = Barn(identitetsnummer = "12345678991".somIdentitetsnummer(), fødselsdato = "2006-05-01".dato()),
                kilder = setOf(Kilde(id = "foo2", type = "OmsorgspengerRammemeldinger[bar2]"))
            )
        )

        val faktisk = runBlocking { rammemeldingerGateway.hentAleneOmsorg(
            saksnummer = "SAKMED".somOmsorgspengerSaksnummer(),
            correlationId = CorrelationId.genererCorrelationId()
        )}

        assertThat(faktisk).hasSameElementsAs(forventet)
    }

    @Test
    fun `Person uten alene om omsorgen registrert som rammemelding`() {
        val faktisk = runBlocking { rammemeldingerGateway.hentAleneOmsorg(
            saksnummer = "SAKUTEN".somOmsorgspengerSaksnummer(),
            correlationId = CorrelationId.genererCorrelationId()
        )}

        assertThat(faktisk).isEmpty()
    }
}