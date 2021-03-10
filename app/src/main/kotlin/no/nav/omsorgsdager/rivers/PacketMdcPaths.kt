package no.nav.omsorgsdager.rivers

internal fun packetMdcPath(behov: String) = mapOf(
    "k9_saksnummer" to "@behov.$behov.saksnummer",
    "k9_behandling_id" to "@behov.$behov.behandlingId"
)