package me.devnatan.dockerkt.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
public data class PortBinding(
    @SerialName("HostIp") public var ip: String? = null,
    @SerialName("HostPort") public var port:
        @Serializable(with = UShortAsStringSerializer::class)
        UShort? = null,
)

internal object UShortAsStringSerializer : KSerializer<UShort> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UShortAsString", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: UShort,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UShort {
        val stringValue = decoder.decodeString()
        return stringValue.toUShortOrNull() ?: throw SerializationException("Cannot parse ushort from string")
    }
}

internal object PortBindingsSerializer : KSerializer<Map<ExposedPort, List<PortBinding>?>> {
    private val mapSerializer = MapSerializer(String.serializer(), ListSerializer(PortBinding.serializer()).nullable)

    override val descriptor: SerialDescriptor
        get() = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): Map<ExposedPort, List<PortBinding>?> {
        val map = mapSerializer.deserialize(decoder)
        return map.mapKeys { ExposedPort.fromString(it.key) }
    }

    override fun serialize(
        encoder: Encoder,
        value: Map<ExposedPort, List<PortBinding>?>,
    ) {
        mapSerializer.serialize(encoder, value.mapKeys { it.key.toString() })
    }
}
