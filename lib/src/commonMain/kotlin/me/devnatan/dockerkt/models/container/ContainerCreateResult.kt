package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ContainerCreateResult(
    @SerialName("Id") val id: String,
    @SerialName("Warnings") val warnings: List<String>,
)
