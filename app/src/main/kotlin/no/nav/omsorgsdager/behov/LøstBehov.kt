package no.nav.omsorgsdager.behov

import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.lovverk.Lovanvendelser

internal interface LøstBehov {
    val navn: String
    val versjon : Int
    val lovanvendelser: Lovanvendelser
    val løsning: Json
}

internal data class TidligereLøstBehov(
    override val navn: String,
    override val versjon: Int,
    override val lovanvendelser: Lovanvendelser,
    override val løsning: Json) : LøstBehov

internal fun Collection<LøstBehov>.kanInnvilges() = all { it.lovanvendelser.avslått.isEmpty() }