package me.devnatan.dockerkt.models.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ImageRootFs(
    @SerialName("Type") public val type: String,
    @SerialName("Layers") public val layers: List<String>? = null,
    @SerialName("BaseLayer") public val baseLayer: String? = null,
)
