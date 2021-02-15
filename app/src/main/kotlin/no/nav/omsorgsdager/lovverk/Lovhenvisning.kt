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

    UtÅretBarnetFyller18(
        beskrivelse = "Dersom barnet er kronisk sykt eller funksjonshemmet, gjelder retten til og med det året barnet fyller 18 år.",
        lovhenvisning = "Ftrl. § 9-5 fjerde ledd andre punktum",
        link = URI("https://lovdata.no/nav/folketrygdloven/kap9/%C2%A79-5")
    ),

    DagenFørSøkerenFyller70(
        beskrivelse = "Det ytes ikke stønad til medlem som er fylt 70 år.",
        lovhenvisning = "Ftrl. § 9-3 første ledd andre punktum",
        link = URI("https://lovdata.no/lov/1997-02-28-19/§9-3")
    )
}