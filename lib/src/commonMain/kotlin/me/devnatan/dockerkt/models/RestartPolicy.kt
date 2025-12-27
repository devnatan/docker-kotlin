package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RestartPolicy(
    @SerialName("Name") public var name: String,
    @SerialName("MaximumRetryCount") public var maximumRetryCount: Int,
) {
    public companion object {
        public const val DoNotRestart: String = ""
        public const val AlwaysRestart: String = "always"
        public const val RestartUnlessStopped: String = "unless-stopped"
        public const val RestartOnFailure: String = "on-failure"
    }
}
