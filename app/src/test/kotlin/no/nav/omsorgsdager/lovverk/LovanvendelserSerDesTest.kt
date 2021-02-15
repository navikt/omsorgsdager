package no.nav.omsorgsdager.lovverk

import no.nav.omsorgsdager.Json.Companion.somJson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class LovanvendelserSerDesTest {

    @Test
    fun `Serialisering og deserialisering`() {

        val fraBuilder = Lovanvendelser.Builder()
            .innvilget("lov1", "tekst1")
            .innvilget("lov1", "tekst1")
            .innvilget("lov1", "tekst2")
            .avslått("lov1", "tekst1")
            .avslått("lov1", "tekst1")
            .avslått("lov1", "tekst2")
            .innvilget("lov2","tekst1")
            .avslått("lov3", "tekst1")
            .build()

        @Language("JSON")
        val json = """
            {
              "innvilget": {
                "lov1": ["tekst2", "tekst1"],
                "lov2": ["tekst1"]
              },
              "avslått": {
                "lov1": ["tekst1", "tekst2"],
                "lov3": ["tekst1"] 
              }
            }
        """.trimIndent().somJson()

        val fraJson = Lovanvendelser.fraJson(json)

        assertEquals(fraBuilder, fraJson)

    }
}