package me.devnatan.dockerkt.models.exec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Options for start an exec instance.
 *
 * @property detach If it should detach from the command immediately after exec start.
 * @property tty Allocate a pseudo-TTY.
 */
@Serializable
public data class ExecStartOptions(
    @SerialName("Detach") var detach: Boolean? = null,
    @SerialName("Tty") val tty: Boolean? = null,
)
