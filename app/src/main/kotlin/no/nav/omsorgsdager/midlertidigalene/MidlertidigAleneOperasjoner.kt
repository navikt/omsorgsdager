package no.nav.omsorgsdager.midlertidigalene

import no.nav.omsorgsdager.BehovssekvensId
import no.nav.omsorgsdager.Identitetsnummer
import no.nav.omsorgsdager.Json
import no.nav.omsorgsdager.K9BehandlingId.Companion.somK9BehandlingId
import no.nav.omsorgsdager.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.omsorgsdager.behandling.BehandlingOperasjoner
import no.nav.omsorgsdager.behandling.BehandlingPersonInfo
import no.nav.omsorgsdager.behandling.BehandlingStatus
import no.nav.omsorgsdager.behandling.BehandlingType
import no.nav.omsorgsdager.behandling.NyBehandling
import no.nav.omsorgsdager.behandling.db.DbBehandling
import no.nav.omsorgsdager.parter.Motpart
import no.nav.omsorgsdager.parter.Part
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
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
        behovssekvensId: BehovssekvensId,
        grunnlag: Json,
        personInfo: Map<Identitetsnummer, BehandlingPersonInfo>,
        behandlingStatus: BehandlingStatus): Pair<NyBehandling, List<Part>> {
        val dto = grunnlag.deserialize<DTO>()

        val personInfoForSøkeren = personInfo.entries.first { it.value.aktørId == dto.søker.aktørId.somAktørId() }

        val søkeren = Søker(
            identitetsnummer = personInfoForSøkeren.key,
            omsorgspengerSaksnummer = personInfoForSøkeren.value.saksnummer,
            aktørId = personInfoForSøkeren.value.aktørId
        )

        val personInfoForAnnenForelder = personInfo.entries.first { it.value.aktørId == dto.annenForelder.aktørId.somAktørId() }

        val annenForelder = Motpart(
            identitetsnummer = personInfoForAnnenForelder.key,
            omsorgspengerSaksnummer = personInfoForAnnenForelder.value.saksnummer,
            aktørId = personInfoForAnnenForelder.value.aktørId
        )

        val behandling = NyBehandling(
            behovssekvensId = behovssekvensId,
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
            val aktørId: String
        )
    }
}