package me.devnatan.dockerkt

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

public open class DockerException internal constructor(
    cause: Throwable?,
) : RuntimeException(cause)

public open class DockerResourceException internal constructor(
    cause: Throwable?,
) : DockerException(cause) {
    override val message: String? get() = null
}

public class DockerResponseException internal constructor(
    cause: Throwable?,
    override val message: String?,
    public val statusCode: HttpStatusCode,
) : DockerResourceException(cause)

@Serializable
internal data class GenericDockerErrorResponse(
    val message: String,
)
