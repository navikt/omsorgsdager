# omsorgsdager

## Lokal utvikling
Trenger *gpr.user* & *gpr.key* i gradle.properties for o laste ned dependencies før man kjør tester.
####I MacOS: ~/.gradle/gradle.properties
```
gpr.user=x-access-token
gpr.key=<DIN GIT PAT>
```
Skap en PAT <a href="https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token">her</a> med read packages scope. 

Kjør tester med
```
./gradlew clean test
```

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #sif_omsorgspenger.

