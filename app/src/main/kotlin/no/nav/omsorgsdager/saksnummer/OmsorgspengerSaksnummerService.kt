package no.nav.omsorgsdager.saksnummer

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.parter.db.PartRepository
import java.time.Duration

internal class OmsorgspengerSaksnummerService(
    private val partRepository: PartRepository,
    private val omsorgspengerSakGatway: OmsorgspengerSakGatway) {

    private val cache: Cache<Identitetsnummer, OmsorgspengerSaksnummer> =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(200)
            .build()

    internal suspend fun hentSaksnummer(identitetsnummer: Identitetsnummer, correlationId: CorrelationId) : OmsorgspengerSaksnummer? {
        return cache.getIfPresent(identitetsnummer)
            ?: partRepository.hentOmsorgspengerSaksnummer(
                identitetsnummer = identitetsnummer)
            ?: omsorgspengerSakGatway.hentSaksnummer(
                identitetsnummer = identitetsnummer,
                correlationId = correlationId
        ).also { saksnummer -> saksnummer?.also {
                cache.put(identitetsnummer, it)
        }}
    }
}