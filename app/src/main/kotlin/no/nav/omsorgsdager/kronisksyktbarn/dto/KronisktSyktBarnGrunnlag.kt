package no.nav.omsorgsdager.kronisksyktbarn.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgsdager.BehandlingId
import no.nav.omsorgsdager.Saksnummer
import java.time.ZonedDateTime

data class KronisktSyktBarnGrunnlag(
    val saksnummer: Saksnummer,
    val behandlingId: BehandlingId,
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val barn: Barn
) {
    constructor(node: ObjectNode) : this(
        saksnummer = node["saksnummer"].asText(),
        behandlingId = node["behandlingId"].asText(),
        mottatt = node["mottatt"].asText().let { ZonedDateTime.parse(it) },
        søker = Søker(node["søker"] as ObjectNode),
        barn = Barn(node["barn"] as ObjectNode),
    )
}