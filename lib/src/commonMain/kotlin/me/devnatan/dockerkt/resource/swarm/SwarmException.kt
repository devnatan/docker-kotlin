package me.devnatan.dockerkt.resource.swarm

import me.devnatan.dockerkt.DockerResourceException

public open class SwarmException internal constructor(
    cause: Throwable?,
) : DockerResourceException(cause)

public class NodeNotPartOfSwarmException internal constructor(
    cause: Throwable?,
) : SwarmException(cause)
