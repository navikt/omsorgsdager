package no.nav.omsorgsdager.kronisksyktbarn.dto

import net.minidev.json.annotate.JsonIgnore
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import java.time.ZonedDateTime

internal object OpprettKroniskSyktBarn {
    internal data class Grunnlag private constructor(
        val saksnummer: Saksnummer,
        val behandlingId: BehandlingId,
        val søknadMottatt: ZonedDateTime,
        val tidspunkt: ZonedDateTime = søknadMottatt,
        val søker: Søker,
        val barn: Barn) {
        @get:JsonIgnore
        internal val involverteIdentitetsnummer = setOf(søker.identitetsnummer, barn.identitetsnummer).filterNotNull().toSet()
    }
}