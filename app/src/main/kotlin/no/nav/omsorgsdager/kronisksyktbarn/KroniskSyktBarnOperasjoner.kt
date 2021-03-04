package no.nav.omsorgsdager.kronisksyktbarn

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
import no.nav.omsorgsdager.parter.Barn
import no.nav.omsorgsdager.parter.Part
import no.nav.omsorgsdager.parter.Søker
import no.nav.omsorgsdager.person.AktørId.Companion.somAktørId
import no.nav.omsorgsdager.tid.Periode
import java.time.LocalDate
import java.time.ZonedDateTime

internal object KroniskSyktBarnOperasjoner : BehandlingOperasjoner<KroniskSyktBarnBehandling> {
    override fun mapTilEksisterendeBehandling(
        dbBehandling: DbBehandling,
        parter: List<Part>): KroniskSyktBarnBehandling {
        require(dbBehandling.type == BehandlingType.KRONISK_SYKT_BARN) {
            "Kan ikke mappe ${dbBehandling.type} til KRONISK_SYKT_BARN"
        }

        require(parter.size == 2) {
            "Forventer at behandligner for KRONISK_SYKT_BARN skal ha 2 parter, hadde ${parter.size}"
        }

        return KroniskSyktBarnBehandling(
            k9Saksnummer = dbBehandling.k9Saksnummer,
            k9behandlingId = dbBehandling.k9behandlingId,
            tidspunkt = dbBehandling.tidspunkt,
            status = dbBehandling.status,
            periode = dbBehandling.periode,
            søker = parter.first { it is Søker } as Søker,
            barn = parter.first { it is Barn } as Barn
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

        val personInfoForBarnet = personInfo.entries.first { it.value.aktørId == dto.barn.aktørId.somAktørId() }

        val barnet = Barn(
            identitetsnummer = personInfoForBarnet.key,
            fødselsdato = personInfoForBarnet.value.fødselsdato,
            omsorgspengerSaksnummer = personInfoForBarnet.value.saksnummer,
            aktørId = personInfoForBarnet.value.aktørId
        )

        val behandling = NyBehandling(
            behovssekvensId = behovssekvensId,
            saksnummer = dto.saksnummer.somK9Saksnummer(),
            behandlingId = dto.behandlingId.somK9BehandlingId(),
            tidspunkt = dto.tidspunkt,
            periode = Periode(
                fom = dto.periode.fom,
                tom = dto.periode.tom
            ),
            type = BehandlingType.KRONISK_SYKT_BARN,
            grunnlag = grunnlag,
            status = behandlingStatus
        )

        return behandling to listOf(søkeren, barnet)
    }

    private data class DTO(
        val saksnummer: String,
        val behandlingId: String,
        val tidspunkt: ZonedDateTime,
        val periode: DTOPeriode,
        val søker: Person,
        val barn: Person) {
        data class Person(
            val aktørId: String
        )
        data class DTOPeriode(
            val fom: LocalDate,
            val tom: LocalDate
        )
    }
}