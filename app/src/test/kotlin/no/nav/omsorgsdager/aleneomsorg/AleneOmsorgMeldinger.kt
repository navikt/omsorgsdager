package no.nav.omsorgsdager.AleneOmsorg

import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgsdager.*
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.person.AktørId
import no.nav.omsorgsdager.testutils.mocketK9Saksnummer
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal object AleneOmsorgMeldinger {

    internal fun melding(
        saksnummer: String = "${mocketK9Saksnummer()}",
        behandlingId: String = "${K9BehandlingId.generateK9BehandlingId()}",
        søkersAktørId: AktørId,
        barnsAktørId: AktørId,
        tidspunkt: ZonedDateTime = ZonedDateTime.now(),
        periode: Periode = Periode("2020-01-01/2025-12-31")
    ) = """
        {
            "versjon": "1.0.0",
            "saksnummer": "$saksnummer",
            "behandlingId": "$behandlingId",
            "tidspunkt": "$tidspunkt",
            "søker": {
                "aktørId": "$søkersAktørId"
            },
            "barn": {
                "aktørId": "$barnsAktørId"
            },
            "periode": {
                "fom": "${periode.fom}",
                "tom": "${periode.tom}"
            }
        }
    """.trimIndent().somJson()

    internal fun innvilget(
        melding: Json
    ) = Behovssekvens(
        id = "${BehovssekvensId.genererBehovssekvensId()}",
        correlationId = "${CorrelationId.genererCorrelationId()}",
        behov = arrayOf(Behov(navn = "InnvilgetAleneOmsorg", input = melding.map))
    )

    internal fun avslått(
        melding: Json
    ) = Behovssekvens(
        id = "${BehovssekvensId.genererBehovssekvensId()}",
        correlationId = "${CorrelationId.genererCorrelationId()}",
        behov = arrayOf(Behov(navn = "AvslåttAleneOmsorg", input = melding.map))
    )
}