package no.nav.omsorgsdager.midlertidigalene.dto

import net.minidev.json.annotate.JsonIgnore
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import no.nav.omsorgsdager.tid.Periode
import java.time.ZonedDateTime

internal object OpprettMidlertidigAlene {
    internal data class Grunnlag private constructor(
        val saksnummer: Saksnummer,
        val behandlingId: BehandlingId,
        val søknadMottatt: ZonedDateTime,
        val tidspunkt: ZonedDateTime = Periode.utcNå(),
        val søker: Søker,
        val motpart: Motpart) {
        @get:JsonIgnore
        internal val involverteIdentitetsnummer = setOf(søker.identitetsnummer, motpart.identitetsnummer)
    }
}