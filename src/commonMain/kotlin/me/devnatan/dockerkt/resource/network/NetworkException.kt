package me.devnatan.dockerkt.resource.network

import me.devnatan.dockerkt.DockerResourceException

public open class NetworkException internal constructor(
    cause: Throwable?,
) : DockerResourceException(cause)

public class NetworkConflictException internal constructor(
    cause: Throwable?,
    public val networkName: String,
) : NetworkException(cause)

public class NetworkForbiddenException internal constructor(
    cause: Throwable?,
    public val networkId: String,
) : NetworkException(cause)

public class NetworkNotFoundException internal constructor(
    cause: Throwable?,
    public val networkName: String,
) : NetworkException(cause)

public class NetworkInUseException(
    cause: Throwable,
    public val networkId: String,
) : NetworkException(cause)
