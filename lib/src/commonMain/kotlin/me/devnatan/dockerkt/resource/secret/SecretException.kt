package me.devnatan.dockerkt.resource.secret

import me.devnatan.dockerkt.DockerResourceException

public open class SecretException internal constructor(
    cause: Throwable?,
) : DockerResourceException(cause)

public class SecretNotFoundException internal constructor(
    cause: Throwable?,
    public val secretId: String,
    public val version: Long,
) : SecretException(cause)

public class SecretNameConflictException internal constructor(
    cause: Throwable?,
    public val secretName: String?,
) : SecretException(cause)
