package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IdOnlyResponse(
    @SerialName("Id") val id: String,
)
