package me.devnatan.dockerkt.models.secret

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SecretSpec(
    @SerialName("Name") val name: String? = null,
    @SerialName("Data") val data: String? = null,
    @SerialName("Driver") val driver: Driver? = null,
    @SerialName("Templating") val templating: Driver? = null,
) {
    @Serializable
    public data class Driver(
        @SerialName("Name") val name: String,
        @SerialName("Options") val options: Map<String, String>? = null,
    )
}
