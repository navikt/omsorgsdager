package no.nav.omsorgsdager.person

import no.nav.omsorgsdager.CorrelationId

internal interface PersonInfoGateway {
    suspend fun hent(aktørIder: Set<AktørId>, correlationId: CorrelationId) : Map<AktørId, PersonInfo>
}