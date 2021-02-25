package no.nav.omsorgsdager.midlertidigalene

import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgsdager.BehovssekvensId
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime
import java.util.*

internal object MidlertidigAleneMeldinger {

    internal fun melding(
        saksnummer: String = UUID.randomUUID().toString().takeLast(10),
        behandlingId: String = UUID.randomUUID().toString(),
        søkersIdentitetsnummer: String,
        annenForelderIdentitetsnummer: String,
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
            "annenForelder": {
                "identitetsnummer": "$annenForelderIdentitetsnummer"
            },
            "gyldigFraOgMed": "${periode.fom}",
            "gyldigTilOgMed": "${periode.tom}"
        }
    """.trimIndent().somJson()

    internal fun innvilget(
        melding: Json
    ) = Behovssekvens(
        id = "${BehovssekvensId.genererBehovssekvensId()}",
        correlationId = "CallId_${UUID.randomUUID()}",
        behov = arrayOf(Behov(navn = "InnvilgetMidlertidigAlene", input = melding.map))
    )

    internal fun avslått(
        melding: Json
    ) = Behovssekvens(
        id = "${BehovssekvensId.genererBehovssekvensId()}",
        correlationId = "CallId_${UUID.randomUUID()}",
        behov = arrayOf(Behov(navn = "AvslåttMidlertidigAlene", input = melding.map))
    )
}