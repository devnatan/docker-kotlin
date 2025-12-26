package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ThrottleDevice(
    @SerialName("Path") public var path: String,
    // TODO check if it's really int64
    @SerialName("Rate") public var rate: Long,
)
