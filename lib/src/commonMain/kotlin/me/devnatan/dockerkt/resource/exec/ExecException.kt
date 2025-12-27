package me.devnatan.dockerkt.resource.exec

import me.devnatan.dockerkt.DockerResourceException

public open class ExecException internal constructor(
    cause: Throwable?,
) : DockerResourceException(cause)

public class ExecNotFoundException internal constructor(
    cause: Throwable?,
    public val execId: String,
) : ExecException(cause)
