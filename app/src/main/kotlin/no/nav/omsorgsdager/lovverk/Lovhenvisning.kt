package no.nav.omsorgsdager.lovverk

import java.net.URI

internal interface Lovhenvisning {
    val lovhenvisning: String
}

internal enum class Folketrygdeloven(
    private val beskrivelse: String,
    private val link: URI,
    override val lovhenvisning: String) : Lovhenvisning {

    KroniskSyktBarn(
        beskrivelse = "Dersom en arbeidstaker har kronisk syke eller funksjonshemmete barn og dette fører til en markert høyere risiko for fravær fra arbeidet",
        lovhenvisning = "Ftrl. § 9-6 andre ledd",
        link = URI("https://lovdata.no/nav/folketrygdloven/kap9/%C2%A79-6")
    ),

    MidlertidigAlene(
        beskrivelse = "Arbeidstakeren regnes for å ha aleneomsorg for et barn også hvis den andre av barnets foreldre i lang tid ikke kan ha tilsyn med barnet fordi han eller hun er funksjonshemmet, innlagt i helseinstitusjon e.l.",
        lovhenvisning = "Ftrl. § 9-6 tredje ledd",
        link = URI("https://lovdata.no/nav/folketrygdloven/kap9/%C2%A79-6")
    ),

    UtÅretBarnetFyller18(
        beskrivelse = "Dersom barnet er kronisk sykt eller funksjonshemmet, gjelder retten til og med det året barnet fyller 18 år.",
        lovhenvisning = "Ftrl. § 9-5 fjerde ledd andre punktum",
        link = URI("https://lovdata.no/nav/folketrygdloven/kap9/%C2%A79-5")
    )
}