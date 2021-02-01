package no.nav.omsorgsdager.config

internal typealias Environment = Map<String, String>

internal fun Environment.harEnv(key: String) = containsKey(key)

internal fun Environment.hentRequiredEnv(key: String) : String = requireNotNull(get(key)) {
    "Environment variable $key må være satt"
}

internal fun Environment.hentOptionalEnv(key: String) : String? = get(key)
internal fun String.csv() = replace(" ", "").split(",").toSet()