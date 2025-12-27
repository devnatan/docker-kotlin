package me.devnatan.dockerkt.models.volume

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Volume(
    @SerialName("Name") public val name: String,
    @SerialName("Driver") public val driver: String,
    @SerialName("Scope") public val scope: String,
    @SerialName("Mountpoint") public val mountPoint: String,
    @SerialName("CreatedAt") public val createdAt: String?,
    @SerialName("Labels") public val labels: Map<String, String>?,
    @SerialName("Options") public val options: Map<String, String>?,
)
