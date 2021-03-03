package no.nav.omsorgsdager.person

import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.testutils.somMockedIdentetsnummer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class MockedPersonInfoGateway : PersonInfoGateway {
    override suspend fun hent(aktørIder: Set<AktørId>, correlationId: CorrelationId): Map<AktørId, PersonInfo> {
        return aktørIder.associateWith { aktørId -> PersonInfo(
            identitetsnummer = aktørId.somMockedIdentetsnummer(),
            fødselsdato = LocalDate.from(ddMMyy.parse("$aktørId".subSequence(0,6))) // Seks første siffer i aktørId må være en dato vi kan parse
        )}
    }

    override suspend fun check() = Healthy("MockedPersonInfoGateway", "OK")

    private companion object {
        private val ddMMyy = DateTimeFormatter.ofPattern("ddMMyy")
    }
}