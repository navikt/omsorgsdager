package no.nav.omsorgsdager.vedtak

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.omsorgsdager.ApplicationContext
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.testutils.ApplicationContextExtension.Companion.buildStarted
import no.nav.omsorgsdager.testutils.somMocketOmsorgspengerSaksnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.vedtak.dto.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

internal data class InnvilgedeVedtakRequest(
    private val identitetsnummer: String,
    private val fom: String,
    private val tom: String) {
    constructor(identitetsnummer: Identitetsnummer, periode: Periode) : this(
        identitetsnummer = "$identitetsnummer",
        fom = "${periode.fom}",
        tom = "${periode.tom}"
    )
    internal val jsonRequest = """{ "fom": "$fom", "tom": "$tom", "identitetsnummer": "$identitetsnummer"}""".trimIndent().somJson()
}
internal abstract class InnvilgedeVedtakKontrakt(
    applicationContextBuilder: ApplicationContext.Builder) {


    protected val applicationContext = applicationContextBuilder.also {
        it.innvilgedeVedtakService = mockk<InnvilgedeVedtakService>().also {
            coEvery { it.hentInnvilgedeVedtak(eq(IdentitetsnummerUtenVedtak), any(), any()) }.returns(InnvilgedeVedtak(emptyList()))
            coEvery { it.hentInnvilgedeVedtak(eq(IdentitetsnummerMedToAvHver), any(), any()) }.returns(InnvilgedeVedtak(
                kroniskSyktBarn = listOf(
                    KroniskSyktBarnInnvilgetVedtak(barn = Barn(identitetsnummer = IdentitetsnummerBarn1, fødselsdato = LocalDate.parse("2020-01-01"), omsorgspengerSaksnummer = IdentitetsnummerBarn1.somMocketOmsorgspengerSaksnummer()), tidspunkt = ZonedDateTime.parse("2020-11-10T12:00:00.00Z"), periode = Periode("2020-01-01/2020-12-31"), kilder = setOf(Kilde(id ="1", type = "K9-Sak"))),
                    KroniskSyktBarnInnvilgetVedtak(barn = Barn(identitetsnummer = null, fødselsdato = LocalDate.parse("2019-01-01")), tidspunkt = ZonedDateTime.parse("2021-02-19T23:30:00.00Z"), periode = Periode("2018-01-01/2025-12-31"), kilder = setOf(Kilde(id ="1", type = "Infotrygd")))
                ),
                midlertidigAlene = listOf(
                    MidlertidigAleneInnvilgetVedtak(tidspunkt = ZonedDateTime.parse("1999-11-10T12:00:00.00Z"), periode = Periode(LocalDate.parse("2005-01-01")), kilder = setOf(Kilde(id = "3", type = "Infotrygd"))),
                    MidlertidigAleneInnvilgetVedtak(tidspunkt = ZonedDateTime.parse("2020-11-10T12:00:00.00Z"), periode = Periode("2020-05-05/2030-03-03"), kilder = setOf(Kilde(id = "4", type = "K9-Sak")))
                )
            ))

        }
    }.buildStarted()

    @Test
    fun `Person som ikke har vedtak`() {
        assertEquals(ForventetResponseUtenVedtak, hentInnvilgedeVedtak(RequestUtenVedtak))
    }

    @Test
    fun `Person som har 2 av hvert vedtak`() {
        assertEquals(ForventetResponseMedToAvHver, hentInnvilgedeVedtak(RequestMedToAvHver))
    }

    @Test
    fun `Person som har 2 av hvert vedtak requestes med tidenes ende`() {
        assertEquals(ForventetResponseMedToAvHver, hentInnvilgedeVedtak(RequestMedToAvHverTidenesEnde))
    }

    abstract fun hentInnvilgedeVedtak(jsonRequest: Json) : Json
    protected fun requestUtenVedtak() = RequestUtenVedtak

    protected companion object {
        private val IdentitetsnummerBarn1 = "11111111111".somIdentitetsnummer()

        protected val IdentitetsnummerUtenVedtak = "29099011111".somIdentitetsnummer()
        private val RequestUtenVedtak = InnvilgedeVedtakRequest(identitetsnummer = IdentitetsnummerUtenVedtak, periode = Periode(2021)).jsonRequest
        private val ForventetResponseUtenVedtak = """{"kroniskSyktBarn": [], "midlertidigAlene": [], "aleneOmsorg": []}""".somJson()

        private val IdentitetsnummerMedToAvHver = "29099011110".somIdentitetsnummer()
        private val RequestMedToAvHver = InnvilgedeVedtakRequest(identitetsnummer = IdentitetsnummerMedToAvHver, periode = Periode(2021)).jsonRequest
        private val RequestMedToAvHverTidenesEnde = InnvilgedeVedtakRequest(identitetsnummer = IdentitetsnummerMedToAvHver, periode = Periode("2021-01-01/9999-12-31")).jsonRequest

        @Language("JSON")
        private val ForventetResponseMedToAvHver = """
        {
            "kroniskSyktBarn": [{
                "barn": {
                    "identitetsnummer": "11111111111",
                    "fødselsdato": "2020-01-01",
                    "omsorgspengerSaksnummer": "OP11111111111"
                },
                "kilder": [{
                    "id": "1",
                    "type": "K9-Sak"
                }],
                "vedtatt": "2020-11-10",
                "gyldigFraOgMed": "2020-01-01",
                "gyldigTilOgMed": "2020-12-31"
            }, {
                "barn": {
                    "identitetsnummer": null,
                    "fødselsdato": "2019-01-01",
                    "omsorgspengerSaksnummer": null
                },
                "kilder": [{
                    "id": "1",
                    "type": "Infotrygd"
                }],
                "vedtatt": "2021-02-20",
                "gyldigFraOgMed": "2018-01-01",
                "gyldigTilOgMed": "2025-12-31"
            }],
            "midlertidigAlene": [{
                "kilder": [{
                    "id": "3",
                    "type": "Infotrygd"
                }],
                "vedtatt": "1999-11-10",
                "gyldigFraOgMed": "2005-01-01",
                "gyldigTilOgMed": "2005-01-01"
            }, {
                "kilder": [{
                    "id": "4",
                    "type": "K9-Sak"
                }],
                "vedtatt": "2020-11-10",
                "gyldigFraOgMed": "2020-05-05",
                "gyldigTilOgMed": "2030-03-03"
            }],
            "aleneOmsorg": []
        }
        """.trimIndent().somJson()
    }
}