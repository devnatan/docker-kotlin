package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class BlkioWeightDevice(
    @SerialName("Path") public var path: String,
    @SerialName("Weight") public var weight: Int,
)
