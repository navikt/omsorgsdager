package no.nav.omsorgsdager.vedtak

import kotlinx.coroutines.runBlocking
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.Kilde
import no.nav.omsorgsdager.vedtak.infotrygd.KroniskSyktBarnInfotrygdInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.infotrygd.MidlertidigAleneInfotrygdInnvilgetVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(ApplicationContextExtension::class)
internal class OmsorgspengerInfotrygdRammevedtakGatewayTest(
    applicationContextBuilder: ApplicationContext.Builder) {

    private val applicationContext = applicationContextBuilder.build()
    private val omsorgspengerInfotrygdRammevedtakGateway = applicationContext.omsorgspengerInfotrygdRammevedtakGateway

    @Test
    fun `Person med utvidetrett & midlertidigalene i infotrygd`() {
        val identitetsnummerMedVedtakInfotrygd = "29099022222".somIdentitetsnummer()

        val forventetResponseMedInfotrygdVedtak = listOf(
            KroniskSyktBarnInfotrygdInnvilgetVedtak(
                vedtatt = LocalDate.parse("2020-06-21"),
                kilder = setOf(Kilde(id="UTV.RETT/20D/29099022222", type= "Personkort")),
                gyldigFraOgMed = LocalDate.parse("2020-06-21"),
                gyldigTilOgMed = LocalDate.parse("2020-06-21"),
                barnetsIdentitetsnummer = "01019911111".somIdentitetsnummer(),
                barnetsFødselsdato = LocalDate.parse("1999-01-01")
            ),
            KroniskSyktBarnInfotrygdInnvilgetVedtak(
                vedtatt = LocalDate.parse("2020-06-22"),
                kilder = setOf(Kilde(id="UTV.RETT/20D/010199", type= "Personkort")),
                gyldigFraOgMed = LocalDate.parse("2020-06-22"),
                gyldigTilOgMed = LocalDate.parse("2020-06-25"),
                barnetsIdentitetsnummer = null,
                barnetsFødselsdato = LocalDate.parse("1999-01-01")
            ),
            MidlertidigAleneInfotrygdInnvilgetVedtak(
                vedtatt = LocalDate.parse("1998-06-21"),
                kilder = setOf(Kilde(id="midl.alene.om/17D", type = "Personkort")),
                gyldigFraOgMed = LocalDate.parse("1998-06-25"),
                gyldigTilOgMed = LocalDate.parse("2001-06-25")
            )
        )

        val resultat = runBlocking {
            // WireMock svarer på identitetsnummer = 29099022222, any, any
            omsorgspengerInfotrygdRammevedtakGateway.hentInnvilgedeVedtak(
                identitetsnummer = identitetsnummerMedVedtakInfotrygd,
                periode = Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)),
                correlationId = CorrelationId.genererCorrelationId()
            )
        }

        assertThat(resultat).hasSameElementsAs(forventetResponseMedInfotrygdVedtak)
    }
}