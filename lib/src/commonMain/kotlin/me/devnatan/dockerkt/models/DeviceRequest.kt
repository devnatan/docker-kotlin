package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class DeviceRequest(
    @SerialName("Driver") public var driver: String? = null,
    @SerialName("Count") public var count: Int? = null,
    @SerialName("DeviceIDs") public var deviceIDs: List<String>? = null,
    @SerialName("Capabilities") public var capabilities: List<String>? = null,
    @SerialName("Options") public var options: Map<String, String?>? = null,
)
