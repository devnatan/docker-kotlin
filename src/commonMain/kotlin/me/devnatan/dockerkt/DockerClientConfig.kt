package me.devnatan.dockerkt

import me.devnatan.dockerkt.io.DefaultDockerHttpSocket
import me.devnatan.dockerkt.io.DefaultDockerUnixSocket
import me.devnatan.dockerkt.io.TcpSocketPrefix
import me.devnatan.dockerkt.io.UnixSocketPrefix
import kotlin.jvm.JvmStatic

internal val DefaultDocketClientConfig = DocketClientConfig.builder().forCurrentPlatform().build()

/**
 * Daemon socket to connect to.
 */
private const val DockerHostEnvKey = "DOCKER_HOST"

/**
 * Override the negotiated Docker Remote API version.
 */
private const val DockerApiVersionEnvKey = "DOCKER_API_VERSION"

/**
 * Minimum Docker Remote API version supported by docker-kotlin.
 */
public const val DefaultDockerApiVersion: String = "1.41"

/**
 * Class to store all Docker client configurations.
 *
 * @param socketPath Docker socket file used to communicate with the main Docker daemon.
 *                   If not set, it will try to get from [DockerHostEnvKey] environment variable, if it's found,
 *                   will try to select the socket path based on current operating system.
 * @param apiVersion The version of the Docker API that will be used during communication.
 *                   See more: [Versioned API and SDK](https://docs.docker.com/engine/api/#versioned-api-and-sdk).
 */
public class DocketClientConfig(
    public val socketPath: String,
    public val apiVersion: String,
    public val debugHttpCalls: Boolean,
) {
    init {
        check(socketPath.isNotBlank()) { "Socket path must be provided and cannot be blank" }
        check(apiVersion.isNotBlank()) { "Docker Remote API version must be provided and cannot be blank" }
    }

    public companion object {
        @JvmStatic
        public fun builder(): DockerClientConfigBuilder = DockerClientConfigBuilder()
    }
}

/**
 * Mutable builder for Docker client configuration.
 */
public class DockerClientConfigBuilder {
    /**
     * Docker socket file used to communicate with main Docker daemon.
     */
    private var socketPath: String = ""

    /**
     * The version of the Docker API that will be used during communication.
     */
    private var apiVersion: String =
        envOrFallback(
            key = DockerApiVersionEnvKey,
            fallback = DefaultDockerApiVersion,
            prefix = null,
        )

    /**
     * Whether to debug the HTTP calls to the Docker daemon.
     */
    private var debugHttpCalls: Boolean = false

    /**
     * Sets the Docker socket path.
     *
     * @param socketPath The Docker socket path.
     */
    public fun socketPath(socketPath: String): DockerClientConfigBuilder {
        this.socketPath = socketPath
        return this
    }

    /**
     * Sets the target Docker Remote API version.
     *
     * @param apiVersion Target Docker Remote API version.
     */
    public fun apiVersion(apiVersion: String): DockerClientConfigBuilder {
        this.apiVersion = apiVersion
        return this
    }

    /**
     * Sets the debug logging of HTTP calls.
     *
     * @param debugHttpCalls whether to log the HTTP calls
     */
    public fun debugHttpCalls(debugHttpCalls: Boolean = true): DockerClientConfigBuilder {
        this.debugHttpCalls = debugHttpCalls
        return this
    }

    /**
     * Configures to use a Unix socket defaults common to the standard Docker configuration.
     *
     * The socket path is defined to [DefaultDockerUnixSocket] if `DOCKER_HOST` env var is not set, or it doesn't
     * have the [UnixSocketPrefix] on its prefix.
     */
    public fun useUnixDefaults(): DockerClientConfigBuilder {
        socketPath =
            envOrFallback(
                key = DockerHostEnvKey,
                fallback = DefaultDockerUnixSocket,
                prefix = UnixSocketPrefix,
            )
        return this
    }

    /**
     * Configures to use an HTTP socket defaults common to the standard Docker configuration.
     *
     * The socket path is defined to [DefaultDockerHttpSocket] if `DOCKER_HOST` env var is not set, or it doesn't
     * have the [TcpSocketPrefix] on its prefix.
     */
    public fun useHttpDefaults(): DockerClientConfigBuilder {
        socketPath =
            envOrFallback(
                key = DockerHostEnvKey,
                fallback = DefaultDockerHttpSocket,
                prefix = TcpSocketPrefix,
            )
        return this
    }

    /**
     * Configures the [socketPath] based on the current platform.
     * See [selectDockerSocketPath] for implementation details.
     */
    public fun forCurrentPlatform(): DockerClientConfigBuilder {
        socketPath =
            envOrFallback(
                key = DockerHostEnvKey,
                fallback = selectDockerSocketPath(),
                prefix = null,
            )
        return this
    }

    /**
     * Builds this class to a [DocketClientConfig].
     */
    public fun build(): DocketClientConfig = DocketClientConfig(socketPath, apiVersion, debugHttpCalls)

    /**
     * Returns the value for the given environment variable [key] or [fallback] if it isn't set.
     *
     * @param key The environment variable key.
     * @param fallback Fallback value if environment key is not set, or it's value don't start with [prefix].
     * @param prefix Prefix to check if the environment variable starts with.
     */
    private fun envOrFallback(
        key: String,
        fallback: String,
        prefix: String?,
    ): String =
        env(key)?.ifBlank { null }?.let { path ->
            if (prefix == null || path.startsWith(prefix)) {
                path
            } else {
                null
            }
        } ?: fallback

    /**
     * Selects a Docker socket path based on current OS.
     */
    private fun selectDockerSocketPath(): String =
        if (isUnixPlatform()) {
            DefaultDockerUnixSocket
        } else {
            DefaultDockerHttpSocket
        }
}
