package no.nav.omsorgsdager.tilgangsstyring

import io.ktor.application.*
import no.nav.helse.dusseldorf.ktor.core.DefaultProblemDetails
import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdager.correlationId
import org.slf4j.LoggerFactory

internal class Tilgangsstyring(
    private val tokenResolver: TokenResolver,
    private val omsorgspengerTilgangsstyringGateway: OmsorgspengerTilgangsstyringGateway) {

    internal suspend fun verifiserTilgang(call: ApplicationCall, operasjon: Operasjon) {
        require(operasjon.identitetsnummer.isNotEmpty()) { "Må settes minst et identitetsnummer." }
        val token = tokenResolver.resolve(call)
        val correlationId = call.correlationId()
        when (token.erPersonToken) {
            true -> {
                logger.info("Sjekker om person har har tilgang.")
                if (!omsorgspengerTilgangsstyringGateway.harTilgang(
                        token = token,
                        operasjon = operasjon,
                        correlationId= correlationId)) {
                    logger.warn("Personen kan ikke gjøre operasjonen $operasjon")
                    throw Throwblem(problemDetails)
                }
            }
            false -> {
                logger.info("Sjekker om system har tilgang.")
                if (!token.harTilgangSomSystem) {
                    logger.warn("Systemet ${token.clientId} har ikke tilgang til tjenesten. Forsøkte $operasjon")
                    throw Throwblem(problemDetails)
                }
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Tilgangsstyring::class.java)
        private val problemDetails = DefaultProblemDetails(
            title = "unauthorized",
            status = 403,
            detail = "Requesten inneholder ikke tilstrekkelige tilganger."
        )
    }
}