package no.nav.omsorgsdager.vedtak

import io.ktor.server.testing.*
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.omsorgsdager
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import no.nav.omsorgsdager.vedtak.InnvilgedeVedtakApisTest.Companion.hentOgAssertInnvilgedeVedtak
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class OmsorgspengerInfotrygdRammevedtakTest(
    applicationContextBuilder: ApplicationContext.Builder
) {

    private val applicationContext = applicationContextBuilder.build()

    @Test
    fun `Person med utvidetrett i infotrygd`() {
        val IdentitetsnummerMedVedtakInfotrygd = "29099022222"

        @Language("JSON")
        val ForventetResponseMedInfotrygdVedtak = """
            {
               "kroniskSyktBarn":[
                  {
                     "barn":{
                        "identitetsnummer":"01019911111",
                        "fødselsdato":"1999-01-01"
                     },
                     "kilder":[
                        {
                           "id":"UTV.RETT/20D/29099022222",
                           "type":"Personkort"
                        }
                     ],
                     "gyldigFraOgMed":"2020-06-21",
                     "gyldigTilOgMed":"2020-06-21",
                     "vedtatt":"2020-06-21"
                  }
               ],
               "midlertidigAlene":[
                  {
                     "kilder":[
                        {
                           "id":"midl.alene.om/17D",
                           "type":"Personkort"
                        }
                     ],
                     "gyldigFraOgMed":"1998-06-25",
                     "gyldigTilOgMed":"2001-06-25",
                     "vedtatt":"1998-06-21"
                  }
               ]
            }
        """.trimIndent()
        withTestApplication({ omsorgsdager(applicationContext) }) {
            hentOgAssertInnvilgedeVedtak(
                identitetsnummer = IdentitetsnummerMedVedtakInfotrygd,
                forventetResponse = ForventetResponseMedInfotrygdVedtak
            )

        }
    }
}