package no.nav.omsorgsdager.person

internal data class AktørId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "Ugyldig aktør id" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,30}".toRegex()
        internal fun String.somAktørId() = AktørId(this)
    }
}