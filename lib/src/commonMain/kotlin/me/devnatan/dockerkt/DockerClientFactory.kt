@file:JvmSynthetic

package me.devnatan.dockerkt

import me.devnatan.dockerkt.DockerClientConfigBuilder.Companion.DEFAULT_DOCKER_API_VERSION
import kotlin.jvm.JvmSynthetic

/**
 * Creates a new Docker client instance with platform default socket path and [DEFAULT_DOCKER_API_VERSION]
 * Docker API version that'll be merged with specified configuration.
 *
 * @param configure The client configuration.
 */
public inline fun DockerClient(crossinline configure: DockerClientConfigBuilder.() -> Unit): DockerClient =
    DockerClient(
        DockerClientConfigBuilder()
            .forCurrentPlatform()
            .apply(configure)
            .build(),
    )
