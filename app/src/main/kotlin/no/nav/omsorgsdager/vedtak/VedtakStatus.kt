package no.nav.omsorgsdager.vedtak

internal enum class VedtakStatus {
    FORESLÅTT,
    INNVILGET,
    AVSLÅTT,
    FORKASTET
}

internal fun Vedtak.harEnEndeligStatus() = status != VedtakStatus.FORESLÅTT