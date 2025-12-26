package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class LogConfig(
    @SerialName("Type") public var type: String,
    @SerialName("Config") public var config: Map<String, String>? = null,
)
