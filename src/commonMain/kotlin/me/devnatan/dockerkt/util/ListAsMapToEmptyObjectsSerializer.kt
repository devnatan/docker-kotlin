package me.devnatan.dockerkt.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal open class ListAsMapToEmptyObjectsSerializer<T : Any>(
    serializer: KSerializer<T>,
) : JsonTransformingSerializer<List<T>>(ListSerializer(serializer)) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        JsonArray(element.jsonObject.entries.map { JsonPrimitive(it.key) })

    override fun transformSerialize(element: JsonElement): JsonElement =
        JsonObject(
            element.jsonArray.associate {
                it.jsonPrimitive.content to JsonObject(mapOf())
            },
        )
}
