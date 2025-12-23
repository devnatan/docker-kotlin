package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.flow.Flow
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.container.ContainerListOptions
import me.devnatan.dockerkt.models.container.ContainerLogsOptions
import me.devnatan.dockerkt.models.container.ContainerPruneFilters
import me.devnatan.dockerkt.models.container.ContainerPruneResult
import me.devnatan.dockerkt.models.container.ContainerRemoveOptions
import me.devnatan.dockerkt.models.container.ContainerSummary
import me.devnatan.dockerkt.resource.image.ImageNotFoundException

/**
 * Returns a list of all containers.
 *
 * @param options Options to customize the listing result.
 */
public suspend inline fun ContainerResource.list(options: ContainerListOptions.() -> Unit): List<ContainerSummary> =
    list(ContainerListOptions().apply(options))

/**
 * Creates a new container.
 *
 * @param options Options to customize the container creation.
 * @throws ImageNotFoundException If the image specified does not exist or isn't pulled.
 * @throws ContainerAlreadyExistsException If a container with the same name already exists.
 */
public suspend inline fun ContainerResource.create(options: ContainerCreateOptions.() -> Unit): String =
    create(ContainerCreateOptions().apply(options))

/**
 * Removes a container.
 *
 * @param container The container id to remove.
 * @param options Removal options.
 * @throws ContainerNotFoundException If the container is not found for the specified id.
 * @throws ContainerRemoveConflictException When trying to remove an active container without the `force` option.
 */
public suspend inline fun ContainerResource.remove(
    container: String,
    options: ContainerRemoveOptions.() -> Unit,
): Unit = remove(container, ContainerRemoveOptions().apply(options))

public suspend inline fun ContainerResource.prune(block: ContainerPruneFilters.() -> Unit): ContainerPruneResult =
    prune(ContainerPruneFilters().apply(block))

/**
 * Resizes the TTY for a container.
 *
 * @param container The container id to resize.
 * @param options Resize options like width and height.
 * @throws ContainerNotFoundException If the container is not found.
 * @throws DockerResponseException If the container cannot be resized or if an error occurs in the request.
 */
public suspend inline fun ContainerResource.resizeTTY(
    container: String,
    options: ResizeTTYOptions.() -> Unit,
) {
    resizeTTY(container, ResizeTTYOptions().apply(options))
}

public inline fun ContainerResource.logs(
    id: String,
    block: ContainerLogsOptions.() -> Unit,
): Flow<Frame> {
    return logs(id, ContainerLogsOptions().apply(block))
}

public fun ContainerResource.logs(id: String): Flow<Frame> = logs(
    id,
    options = ContainerLogsOptions(
        follow = true,
        stderr = true,
        stdout = true,
    ),
)
