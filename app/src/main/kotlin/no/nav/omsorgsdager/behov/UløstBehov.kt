package no.nav.omsorgsdager.behov

internal data class UløstBehov (
    internal val navn: String
)

internal fun String.somUløstBehov() = UløstBehov(navn = this)