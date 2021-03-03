package no.nav.omsorgsdager.person

import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.omsorgsdager.CorrelationId

internal interface PersonInfoGateway : HealthCheck {
    suspend fun hent(aktørIder: Set<AktørId>, correlationId: CorrelationId) : Map<AktørId, PersonInfo>
}