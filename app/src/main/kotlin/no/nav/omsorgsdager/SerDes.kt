package no.nav.omsorgsdager

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.application.*
import io.ktor.request.*

internal object SerDes {
    internal fun ObjectMapper.configured() : ObjectMapper {
        registerKotlinModule()
        disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        return this
    }

    private val ObjectMapper = jacksonObjectMapper().configured()

    internal suspend fun ApplicationCall.objectNode() = kotlin.runCatching {
        receive<ObjectNode>()
    }.fold(
        onSuccess = { it },
        onFailure = { throw IllegalStateException("Ugyldig JSON", it) }
    )

    inline fun <reified T> ObjectNode.map() = requireNotNull(ObjectMapper.treeToValue<T>(this))
}