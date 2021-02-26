package no.nav.omsorgsdager

import org.slf4j.LoggerFactory

internal fun secureLogger() = LoggerFactory.getLogger("tjenestekall")
internal val SecureLogger = secureLogger()