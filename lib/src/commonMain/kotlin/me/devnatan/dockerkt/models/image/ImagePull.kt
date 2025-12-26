package me.devnatan.dockerkt.models.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ImagePull(
    public val id: String? = null,
    @SerialName("status") public val statusText: String,
    public val progressDetail: ProgressDetail? = null,
    @SerialName("progress") public val progressText: String? = null,
) {
    @Serializable
    public data class ProgressDetail(
        public val current: Int? = null,
        public val total: Int? = null,
    )
}
