package no.nav.omsorgsdager.vedtak.infotrygd

import no.nav.omsorgsdager.CorrelationId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.tid.Periode
import no.nav.omsorgsdager.tid.Periode.Companion.startenAvDagenOslo
import no.nav.omsorgsdager.vedtak.dto.Barn
import no.nav.omsorgsdager.vedtak.dto.InnvilgedeVedtak
import no.nav.omsorgsdager.vedtak.dto.KroniskSyktBarnInnvilgetVedtak
import no.nav.omsorgsdager.vedtak.dto.MidlertidigAleneInnvilgetVedtak

internal class InfotrygdInnvilgetVedtakService(
    private val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway) {

    internal suspend fun hentInnvilgedeVedtak(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) : InnvilgedeVedtak {
        val fraInfotrygd = omsorgspengerInfotrygdRammevedtakGateway.hentInnvilgedeVedtak(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        )

        val kroniskSyktBarn = fraInfotrygd.filterIsInstance<KroniskSyktBarnInfotrygdInnvilgetVedtak>().map { KroniskSyktBarnInnvilgetVedtak(
            barn = Barn(identitetsnummer = it.barnetsIdentitetsnummer.toString(), fødselsdato = it.barnetsFødselsdato),
            kilder = it.kilder,
            tidspunkt = it.vedtatt.startenAvDagenOslo(),
            periode = Periode(
                fom = it.gyldigFraOgMed,
                tom = it.gyldigTilOgMed
            )
        )}

        val midlertidigAlene = fraInfotrygd.filterIsInstance<MidlertidigAleneInfotrygdInnvilgetVedtak>().map { MidlertidigAleneInnvilgetVedtak(
            kilder = it.kilder,
            tidspunkt = it.vedtatt.startenAvDagenOslo(),
            periode = Periode(
                fom = it.gyldigFraOgMed,
                tom = it.gyldigTilOgMed
            )
        )}

        return InnvilgedeVedtak(
            kroniskSyktBarn = kroniskSyktBarn,
            midlertidigAlene = midlertidigAlene
        )
    }
}