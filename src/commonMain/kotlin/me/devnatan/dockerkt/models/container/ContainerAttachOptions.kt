package me.devnatan.dockerkt.models.container

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Options for attaching to a container.
 *
 * The attach operation allows you to connect to a container's stdin, stdout, and stderr
 * streams, similar to the `docker attach` command.
 *
 * @see <a href="https://docs.docker.com/engine/api/v1.47/#operation/ContainerAttach">Docker API - Container Attach</a>
 */
@Serializable
public data class ContainerAttachOptions(
    /**
     * Attach to stdout.
     * Default: true
     */
    @Transient
    var stdout: Boolean = true,
    /**
     * Attach to stderr.
     * Default: true
     */
    @Transient
    var stderr: Boolean = true,
    /**
     * Attach to stdin.
     * When enabled, allows sending input to the container.
     * Default: false
     */
    @Transient
    var stdin: Boolean = false,
    /**
     * Stream attached streams.
     * If false, returns immediately after attaching.
     * Default: true
     */
    @Transient
    var stream: Boolean = true,
    /**
     * Return logs from container start.
     * Only works if stream=true.
     * Default: false
     */
    @Transient
    var logs: Boolean = false,
    /**
     * Override the key sequence for detaching a container.
     * Format is a single character [a-Z] or ctrl-<value> where <value> is one of:
     * a-z, @, ^, [, , or _.
     * Default: null (uses Docker daemon default: ctrl-p,ctrl-q)
     */
    @Transient
    var detachKeys: String? = null,
)
