package no.nav.omsorgsdager.person

internal interface PersonInfoGateway {
    suspend fun hent(aktørIder: Set<AktørId>) : Map<AktørId, PersonInfo>
}