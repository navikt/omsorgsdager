package no.nav.omsorgsdager.kronisksyktbarn

import no.nav.omsorgsdager.BehovssekvensId
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
import no.nav.omsorgsdager.parter.Barn
import no.nav.omsorgsdager.parter.Part
import no.nav.omsorgsdager.parter.Søker
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
        saksnummer: Map<Identitetsnummer, OmsorgspengerSaksnummer>,
        behandlingStatus: BehandlingStatus): Pair<NyBehandling, List<Part>> {
        val dto = grunnlag.deserialize<DTO>()

        val søkeren = dto.søker.identitetsnummer.somIdentitetsnummer().let { Søker(
            identitetsnummer = it,
            omsorgspengerSaksnummer = saksnummer.getValue(it)
        )}

        val barnet = Barn(identitetsnummer = dto.barn.identitetsnummer?.somIdentitetsnummer(), fødselsdato = dto.barn.fødselsdato)

        val behandling = NyBehandling(
            behovssekvensId = behovssekvensId,
            saksnummer = dto.saksnummer.somK9Saksnummer(),
            behandlingId = dto.behandlingId.somK9BehandlingId(),
            tidspunkt = dto.tidspunkt,
            periode = Periode(
                fom = dto.gyldigFraOgMed,
                tom = dto.gyldigTilOgMed
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
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val søker: Søker,
        val barn: Barn) {
        data class Søker(
            val identitetsnummer: String
        )
        data class Barn(
            val identitetsnummer: String?,
            val fødselsdato: LocalDate
        )
    }
}