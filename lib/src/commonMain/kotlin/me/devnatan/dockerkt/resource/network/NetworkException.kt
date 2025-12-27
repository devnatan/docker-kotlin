package me.devnatan.dockerkt.resource

import me.devnatan.dockerkt.DockerResourceException

public open class NetworkException internal constructor(
    cause: Throwable?,
) : DockerResourceException(cause)

public class NetworkNotFoundException internal constructor(
    cause: Throwable?,
    public val networkId: String,
) : NetworkException(cause)
