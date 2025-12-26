package me.devnatan.dockerkt.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.devnatan.dockerkt.util.ListAsMapToEmptyObjectsSerializer

@Serializable
public data class ExposedPort internal constructor(
    public val port: UShort,
    public val protocol: ExposedPortProtocol = ExposedPortProtocol.TCP,
) {
    override fun toString(): String = "$port/${protocol.toString().lowercase()}"

    public companion object {
        public fun fromString(exposedPort: String): ExposedPort {
            val portAndProtocol = exposedPort.split('/')
            if (portAndProtocol.size != 2) error("Invalid exposed port")

            val port = portAndProtocol.getOrNull(0)?.toUShortOrNull() ?: error("Invalid exposed port")

            val protocolString = portAndProtocol.getOrNull(1) ?: error("Invalid exposed port")
            val protocol =
                runCatching { ExposedPortProtocol.valueOf(protocolString.uppercase()) }
                    .getOrElse { error("Invalid exposed port") }

            return ExposedPort(port, protocol)
        }
    }
}

@Serializable
public enum class ExposedPortProtocol {
    @SerialName("tcp")
    TCP,

    @SerialName("udp")
    UDP,

    @SerialName("sctp")
    SCTP,
}

internal object ExposedPortSerializer : KSerializer<ExposedPort> {
    override val descriptor: SerialDescriptor
        get() =
            buildClassSerialDescriptor("ExposedPort") {
                element("port", UShort.serializer().descriptor)
                element("protocol", ExposedPortProtocol.serializer().descriptor)
            }

    override fun deserialize(decoder: Decoder): ExposedPort {
        try {
            return ExposedPort.fromString(decoder.decodeString())
        } catch (e: Throwable) {
            throw SerializationException("Cannot parse exposed port", e)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: ExposedPort,
    ) {
        encoder.encodeString(value.toString())
    }
}

internal object ExposedPortsSerializer : ListAsMapToEmptyObjectsSerializer<ExposedPort>(ExposedPortSerializer)
