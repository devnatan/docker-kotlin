package me.devnatan.dockerkt.models.container

import kotlinx.serialization.Serializable

@Serializable
public data class ContainerRemoveOptions(
    public var removeAnonymousVolumes: Boolean = false,
    public var force: Boolean = false,
    public var unlink: Boolean = false,
)
