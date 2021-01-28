package no.nav.omsorgsdager.config

typealias Environment = Map<String, String>

fun Environment.harEnv(key: String) = containsKey(key)

fun Environment.hentRequiredEnv(key: String) : String = requireNotNull(get(key)) {
    "Environment variable $key må være satt"
}

fun Environment.hentOptionalEnv(key: String) : String? = get(key)