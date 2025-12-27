package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ResourceLimit(
    @SerialName("Name") public var name: String,
    @SerialName("Soft") public var soft: Int,
    @SerialName("Hard") public var hard: Int,
)
