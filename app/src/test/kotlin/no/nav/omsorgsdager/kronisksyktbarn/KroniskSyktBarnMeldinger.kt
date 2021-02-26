package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgsdager.BehovssekvensId
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.K9BehandlingId
import no.nav.omsorgsdager.testutils.mocketK9Saksnummer
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal object KroniskSyktBarnMeldinger {
    internal fun melding(
        saksnummer: String = "${mocketK9Saksnummer()}",
        behandlingId: String = "${K9BehandlingId.generateK9BehandlingId()}",
        søkersIdentitetsnummer: String,
        barnetsIdentitetsnummer: String,
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
        id = "${BehovssekvensId.genererBehovssekvensId()}",
        correlationId = "${CorrelationId.genererCorrelationId()}",
        behov = arrayOf(Behov(navn = "InnvilgetKroniskSyktBarn", input = melding.map))
    )

    internal fun avslått(
        melding: Json
    ) = Behovssekvens(
        id = "${BehovssekvensId.genererBehovssekvensId()}",
        correlationId = "${CorrelationId.genererCorrelationId()}",
        behov = arrayOf(Behov(navn = "AvslåttKroniskSyktBarn", input = melding.map))
    )
}