package me.devnatan.dockerkt

import kotlinx.coroutines.flow.collect
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.volume.Volume
import me.devnatan.dockerkt.models.volume.VolumeCreateOptions
import me.devnatan.dockerkt.resource.container.create
import me.devnatan.dockerkt.resource.container.remove
import me.devnatan.dockerkt.resource.image.ImageNotFoundException
import me.devnatan.dockerkt.resource.volume.create
import me.devnatan.dockerkt.resource.volume.remove
import kotlin.test.fail

suspend fun <R> DockerClient.withImage(
    imageName: String,
    block: suspend (String) -> R,
): R {
    try {
        images.pull(imageName).collect()
    } catch (e: Throwable) {
        fail("Failed to pull image", e)
    }

    try {
        return block(imageName)
    } finally {
        try {
            images.remove(imageName, force = true)
        } catch (_: ImageNotFoundException) {}
    }
}

suspend fun <R> DockerClient.withContainer(
    image: String,
    options: ContainerCreateOptions.() -> Unit = {},
    block: suspend (String) -> R,
): Unit =
    withImage(image) { imageTag ->
        val containerId: String
        try {
            containerId =
                containers.create {
                    this.image = imageTag
                    apply(options)
                }
        } catch (e: Throwable) {
            fail("Failed to create container", e)
        }

        try {
            block(containerId)
        } finally {
            containers.remove(containerId) {
                force = true
                removeAnonymousVolumes = true
            }
        }
    }

suspend fun <R> DockerClient.withVolume(
    config: VolumeCreateOptions.() -> Unit = {},
    block: suspend (Volume) -> R,
) {
    val volume: Volume =
        try {
            volumes.create(config)
        } catch (e: Throwable) {
            fail("Failed to create volume", e)
        }

    try {
        block(volume)
    } finally {
        volumes.remove(volume.name) {
            force = true
        }
    }
}

/** Make a container started forever by attaching stdin */
fun ContainerCreateOptions.keepStartedForever() {
    attachStdin = true
    tty = true
}

/** Make a container started forever. */
fun ContainerCreateOptions.sleepForever() {
    command = listOf("sleep", "infinity")
}
