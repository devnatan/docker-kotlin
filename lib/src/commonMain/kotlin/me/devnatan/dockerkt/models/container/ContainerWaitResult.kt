package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ContainerWaitResult internal constructor(
    @SerialName("StatusCode") public val statusCode: Int,
    @SerialName("Error") public val error: Error? = null,
) {
    @Serializable
    public data class Error(
        val message: String,
    )
}
