package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.application.*
import io.ktor.request.*
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode

internal class Json private constructor(
    private val objectNode: ObjectNode) {
    internal val map: Map<String, Any?> = ObjectMapper.convertValue(objectNode)
    internal val raw = requireNotNull(objectNode.toString())

    private constructor(jsonString: String) : this(ObjectMapper.readTree(jsonString) as ObjectNode)

    override fun equals(other: Any?) = when (other) {
        !is Json -> false
        else -> JSONCompare.compareJSON(raw, other.raw, JSONCompareMode.NON_EXTENSIBLE).passed()
    }

    internal inline fun <reified T>deserialize() = requireNotNull(ObjectMapper.treeToValue<T>(objectNode)) {
        "Kunne ikke deserialisere Json"
    }

    override fun toString() = raw

    internal companion object {
        internal fun tomJson() = "{}".somJson()

        internal fun String.somJsonOrNull() = kotlin.runCatching { somJson() }.fold(
            onSuccess = { it },
            onFailure = { null }
        )
        internal fun String.somJson() = Json(jsonString = this)
        internal fun JSONObject.somJson() = Json(jsonString = toString())
        internal fun ObjectNode.somJson() = Json(objectNode = this)

        internal suspend fun ApplicationCall.json() = kotlin.runCatching {
            receive<ObjectNode>().somJson()
        }.fold(
            onSuccess = { it },
            onFailure = { throw IllegalStateException("Ugyldig JSON", it) }
        )

        internal fun ObjectMapper.configured() : ObjectMapper {
            registerKotlinModule()
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            return this
        }

        private val ObjectMapper = jacksonObjectMapper().configured()
    }
}