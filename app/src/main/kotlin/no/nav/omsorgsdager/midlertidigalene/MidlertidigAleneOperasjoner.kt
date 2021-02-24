package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.OmsorgspengerSaksnummer
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.behandling.NyBehandling
import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Motpart
import no.nav.omsorgsdager.parter.Part
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.tid.Periode
import java.time.LocalDate
import java.time.ZonedDateTime

internal object MidlertidigAleneOperasjoner : BehandlingOperasjoner<MidlertidigAleneBehandling> {
    override fun mapTilEksisterendeBehandling(
        dbBehandling: DbBehandling,
        parter: List<Part>): MidlertidigAleneBehandling {
        require(dbBehandling.type == BehandlingType.MIDLERTIDIG_ALENE) {
            "Kan ikke mappe ${dbBehandling.type} til MIDLERTIDIG_ALENE"
        }

        require(parter.size == 2) {
            "Forventer at behandligner for MIDLERTIDIG_ALENE skal ha 2 parter, hadde ${parter.size}"
        }

        return MidlertidigAleneBehandling(
            k9Saksnummer = dbBehandling.k9Saksnummer,
            k9behandlingId = dbBehandling.k9behandlingId,
            tidspunkt = dbBehandling.tidspunkt,
            status = dbBehandling.status,
            periode = dbBehandling.periode,
            søker = parter.first { it is Søker } as Søker,
            annenForelder = parter.first { it is Motpart } as Motpart
        )
    }

    override fun mapTilNyBehandling(
        grunnlag: Json,
        saksnummer: Map<Identitetsnummer, OmsorgspengerSaksnummer>,
        behandlingStatus: BehandlingStatus): Pair<NyBehandling, List<Part>> {
        val dto = grunnlag.deserialize<DTO>()

        val søkeren = dto.søker.identitetsnummer.somIdentitetsnummer().let { Søker(
            identitetsnummer = it,
            omsorgspengerSaksnummer = saksnummer.getValue(it)
        )}
        val annenForelder = dto.annenForelder.identitetsnummer.somIdentitetsnummer().let { Motpart(
            identitetsnummer = it,
            omsorgspengerSaksnummer = saksnummer.getValue(it)
        )}

        val behandling = NyBehandling(
            saksnummer = dto.saksnummer.somK9Saksnummer(),
            behandlingId = dto.behandlingId.somK9BehandlingId(),
            tidspunkt = dto.tidspunkt,
            periode = Periode(
                fom = dto.gyldigFraOgMed,
                tom = dto.gyldigTilOgMed
            ),
            type = BehandlingType.MIDLERTIDIG_ALENE,
            grunnlag = grunnlag,
            status = behandlingStatus
        )

        return behandling to listOf(søkeren, annenForelder)
    }

    private data class DTO(
        val saksnummer: String,
        val behandlingId: String,
        val tidspunkt: ZonedDateTime,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val søker: Person,
        val annenForelder: Person) {
        data class Person(
            val identitetsnummer: String
        )
    }
}