package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.omsorgsdager.Json.Companion.somJson
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class JsonTest {

    @Test
    fun `Tom json`() {
        val json = Json.tomJson()
        assertEquals(0, json.map.size)

    }

    @Test
    fun `Teste sammenligning av Json - ignorerer rekkefølge men må ellers være full match`() {
        @Language("JSON")
        val fraString = """
            {
                "boolean": true,
                "array": ["en","to"],
                "object": {
                    "tre": true
                }
            }
        """.trimIndent().somJson()

        val orgJson = JSONObject().also { root ->
            root.put("object", JSONObject().also { it.put("tre", true) })
            root.put("array", JSONArray().also {
                it.put("to")
                it.put("en")
            })
            root.put("boolean", true)
        }

        assertEquals(fraString, orgJson.somJson())
        assertNotEquals(fraString, orgJson.put("foo", 1).somJson())

        val objectMapper = ObjectMapper()
        val jackson = objectMapper.createObjectNode()
        jackson.replace("object", objectMapper.createObjectNode().put("tre", true))
        jackson.replace("array", objectMapper.createArrayNode().add("to").add("en"))
        jackson.put("boolean", true)

        assertEquals(fraString, jackson.somJson())
        jackson.replace("array", objectMapper.createArrayNode().add("to").add("en").add("null"))
        assertNotEquals(fraString, jackson.somJson())
    }
}