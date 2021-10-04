# omsorgsdager


Kafka-tjänst som bygger på <a href="https://github.com/navikt/k9-rapid">k9-rapid</a>.

---
### [Kroniskt Sykt Barn], [Midlertidig Alene] & [Alene om omsorgen] ###
* Två typer av rivers: Initier & Lagre
* Två typer av behov: Innvilget & Avslått
* Fyra rivers per rammemelding, eks. før kroniskt sykt barn, `InitierInnvilgetKroniskSyktBarnRiver`, `LagreInnvilgetKroniskSyktBarnRiver`, `InitierAvslåttKroniskSyktBarnRiver` & `LagreAvslåttKroniskSyktBarnRiver`
* `Initier` henter identitetsnummer & fødselsdato for involverte aktører fra PDL. Legger til behov **HentOmsorgspengerSaksnummer**.
* `Lagre` sjekker etter løsning på **HentOmsorgspengerSaksnummer** og **HentUtvidetRettParter**. Lagrer behandling i lokal postgres-database.


**Behov**
```
 "InnvilgetKroniskSyktBarn":{
     "versjon":"1.0.0",
     "saksnummer":"1dc56fa0f4",
     "behandlingId":"9b94bea8-a571-4ec3-ad9a-d3c4711cb755",
     "tidspunkt":"2021-10-04T08:13:32.178702+02:00[Europe/Oslo]",
     "søker":{
        "aktørId":"29099011111"
     },
     "barn":{
        "aktørId":"29099011112"
     },
     "periode":{
        "fom":"2020-01-01",
        "tom":"2025-12-31"
     }
}
```
**Løsning**
```
"InnvilgetKroniskSyktBarn":{
     "løst":"2021-10-04T06:13:32.635Z"
}
```
---
### Innvilgede Vedtak ###

* `River`: InnvilgedeVedtakRiver
* `Behov`: HentUtvidetRettVedtakV2
* `Integrasjoner`: K9-infotrygd, omsorgspenger-rammemeldinger-infotrygd & omsorgspenger-sak
* `Løsningsbeskrivelse`: Henter og sammenstiller innvilgede vedtak for utvidet rett fra tilgjengelige kilder.

**Behov**
```
 "HentUtvidetRettVedtakV2":{
     "identitetsnummer":"11111111111",
     "fom":"2020-05-05",
     "tom":"2025-12-31",
}
```
**Løsning**
```
"InnvilgetKroniskSyktBarn":{
     "løst":"2021-10-04T06:13:32.635Z"
}
```
---
### REST API: POST /innvilgede-vedtak-utvidet-rett ###
Hente innvilgede vedtak om utvidet rett for periode.  
Krever bearer token, tilgangstyring er implementert m.h.a. <a href="https://github.com/navikt/omsorgspenger-tilgangsstyring">omsorgspenger-tilgangsstyring</a>.

Request
```
{"fom": "2019-01-01", "tom": "2020-05-05", "identitetsnummer": "12345678901"}
```

Response 
```
{
   "kroniskSyktBarn":[
      {
         "barn":{
            "identitetsnummer":"11111111111",
            "fødselsdato":"2020-01-01",
            "omsorgspengerSaksnummer":"OP11111111111"
         },
         "kilder":[
            {
               "id":"1",
               "type":"K9-Sak"
            }
         ],
         "vedtatt":"2020-11-10",
         "gyldigFraOgMed":"2020-01-01",
         "gyldigTilOgMed":"2020-12-31"
      }
   ],
   "midlertidigAlene":[],
   "aleneOmsorg":[]
}
```

---
## Lokal utvikling
Trenger `gpr.user` & `gpr.key` i gradle.properties for o laste ned dependencies før man kjør tester.

**I MacOS: ~/.gradle/gradle.properties**
```
gpr.user=x-access-token
gpr.key=<DIN GIT PAT>
```
Skape en PAT <a href="https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token">her</a> med read packages scope. 
Kjør tester med `./gradlew clean test`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #sif_omsorgspenger.

Dokumentation på integrasjoner i bruk:<br>
<a href="https://navikt.github.io/pdl/">PDL</a> slack: #mfn