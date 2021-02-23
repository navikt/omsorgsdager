package no.nav.omsorgsdager.kronisksyktbarn

import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime
import java.util.*

internal object KroniskSyktBarnMeldinger {
    private val ulid = ULID()

    internal fun melding(
        saksnummer: String = UUID.randomUUID().toString().takeLast(10),
        behandlingId: String = UUID.randomUUID().toString(),
        søkersIdentitetsnummer: String,
        barnetsIdentitetsnummer: String = "11111111111",
        barnetsFødselsdato: String = "2019-05-05",
        tidspunkt: ZonedDateTime = ZonedDateTime.now(),
        periode: Periode = Periode("2020-01-01/2025-12-31")
    ) = """
        {
            "saksnummer": "$saksnummer",
            "behandlingId": "$behandlingId",
            "tidspunkt": "$tidspunkt",
            "søker": {
                "identitetsnummer": "$søkersIdentitetsnummer"
            },
            "barn": {
                "identitetsnummer": "$barnetsIdentitetsnummer",
                "fødselsdato": "$barnetsFødselsdato"
            },
            "gyldigFraOgMed": "${periode.fom}",
            "gyldigTilOgMed": "${periode.tom}"
        }
    """.trimIndent().somJson()

    internal fun innvilget(
        melding: Json
    ) = Behovssekvens(
        id = ulid.nextULID(),
        correlationId = "CallId_${UUID.randomUUID()}",
        behov = arrayOf(Behov(navn = "InnvilgetKroniskSyktBarn", input = melding.map))
    )

    internal fun avslått(
        melding: Json
    ) = Behovssekvens(
        id = ulid.nextULID(),
        correlationId = "CallId_${UUID.randomUUID()}",
        behov = arrayOf(Behov(navn = "AvslåttKroniskSyktBarn", input = melding.map))
    )
}