package me.devnatan.dockerkt.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {
    @Test
    fun `deserialization of nullable api field with default value`() {
        @Serializable
        data class Entity(
            @SerialName("Cmd") val command: List<String> = emptyList(),
        )

        val json = """{"Cmd": null}"""
        val entity = DockerKotlinJson.decodeFromString<Entity>(json)

        assertEquals(
            expected = Entity(command = emptyList()),
            actual = entity,
        )
    }
}
