package no.nav.omsorgsdager.parter.db

import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.parter.Barn
import no.nav.omsorgsdager.parter.Motpart
import no.nav.omsorgsdager.parter.Part
import no.nav.omsorgsdager.parter.Søker
import java.time.LocalDate

internal data class DbPart(
    internal val behandlingId: BehandlingId,
    private val type: Type,
    val foo: Boolean) {
    internal val part :Part = when (type) {
        Type.SØKER -> Søker(identitetsnummer = "TODO".somIdentitetsnummer())
        Type.MOTPART -> Motpart(identitetsnummer = "TODO".somIdentitetsnummer())
        Type.BARN -> Barn(identitetsnummer = "TODO".somIdentitetsnummer(), fødselsdato = LocalDate.now())
    }

    internal enum class Type {
        SØKER,
        MOTPART,
        BARN
    }
}