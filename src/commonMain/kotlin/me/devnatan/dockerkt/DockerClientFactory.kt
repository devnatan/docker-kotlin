@file:JvmSynthetic

package me.devnatan.dockerkt

import kotlin.jvm.JvmSynthetic

/**
 * Creates a new Docker client instance with platform default socket path and [DefaultDockerApiVersion]
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
