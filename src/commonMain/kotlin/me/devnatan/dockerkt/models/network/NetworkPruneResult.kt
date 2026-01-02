package me.devnatan.dockerkt.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class NetworkPruneResult(
    @SerialName("NetworksDeleted")
    val networksDeleted: List<String>? = null,
)