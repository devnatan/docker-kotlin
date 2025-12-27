package me.devnatan.dockerkt.resource.image

import me.devnatan.dockerkt.DockerResourceException

public open class ImageException internal constructor(
    cause: Throwable?,
) : DockerResourceException(cause)

public class ImageNotFoundException internal constructor(
    cause: Throwable?,
    public val image: String,
) : ImageException(cause)
