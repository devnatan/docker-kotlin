package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class DeviceMapping(
    @SerialName("PathOnHost") var pathOnHost: String? = null,
    @SerialName("PathInContainer") var pathInContainer: String? = null,
    @SerialName("CgroupPermissions") var cgroupPermissions: String? = null,
)
