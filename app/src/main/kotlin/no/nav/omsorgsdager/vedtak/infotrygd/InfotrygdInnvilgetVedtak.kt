package no.nav.omsorgsdager.vedtak.infotrygd

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.vedtak.dto.Kilde
import java.time.LocalDate

interface InfotrygdInnvilgetVedtak {
    val vedtatt: LocalDate
    val gyldigFraOgMed: LocalDate
    val gyldigTilOgMed: LocalDate
    val kilder: Set<Kilde>
}

internal data class KroniskSyktBarnInfotrygdInnvilgetVedtak(
    override val vedtatt: LocalDate,
    override val gyldigFraOgMed: LocalDate,
    override val gyldigTilOgMed: LocalDate,
    override val kilder: Set<Kilde>,
    internal val barnetsFødselsdato: LocalDate,
    internal val barnetsIdentitetsnummer: Identitetsnummer?) : InfotrygdInnvilgetVedtak

internal data class MidlertidigAleneInfotrygdInnvilgetVedtak(
    override val vedtatt: LocalDate,
    override val gyldigFraOgMed: LocalDate,
    override val gyldigTilOgMed: LocalDate,
    override val kilder: Set<Kilde>) : InfotrygdInnvilgetVedtak