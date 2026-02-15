@file:OptIn(ExperimentalContracts::class)

package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.flow.Flow
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.container.ContainerAttachOptions
import me.devnatan.dockerkt.models.container.ContainerAttachWebSocketResult
import me.devnatan.dockerkt.models.container.ContainerCopyOptions
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.container.ContainerListOptions
import me.devnatan.dockerkt.models.container.ContainerLogsOptions
import me.devnatan.dockerkt.models.container.ContainerLogsResult
import me.devnatan.dockerkt.models.container.ContainerPruneFilters
import me.devnatan.dockerkt.models.container.ContainerPruneResult
import me.devnatan.dockerkt.models.container.ContainerRemoveOptions
import me.devnatan.dockerkt.models.container.ContainerSummary
import me.devnatan.dockerkt.resource.image.ImageNotFoundException
import kotlin.contracts.ExperimentalContracts

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

/**
 * Get logs from a container.
 *
 * Similar to the `docker logs` command, this retrieves stdout and/or stderr logs
 * from a container. The logs can be returned as a complete string or streamed
 * progressively as a Flow.
 *
 * The stream format (multiplexed vs raw TTY) is automatically detected from the
 * content, so there's no need to specify whether the container uses TTY.
 *
 * @param container Container id or name.
 * @param block Configuration options for log retrieval. See [ContainerLogsOptions].
 * @return [ContainerLogsResult] containing logs based on the options:
 *   - [ContainerLogsResult.Stream] for streaming mode
 *   - [ContainerLogsResult.StreamDemuxed] for streaming with separated stdout/stderr
 *   - [ContainerLogsResult.Complete] for non-streaming mode
 *   - [ContainerLogsResult.CompleteDemuxed] for non-streaming with separated stdout/stderr
 *
 * @throws ContainerNotFoundException If the container is not found.
 * @throws IllegalArgumentException If neither stdout nor stderr is enabled.
 */
public suspend inline fun ContainerResource.logs(
    container: String,
    block: ContainerLogsOptions.() -> Unit,
): ContainerLogsResult = logs(container, ContainerLogsOptions().apply(block))

/**
 * Get logs from a container with [ContainerLogsOptions.follow], [ContainerLogsOptions.demux], [ContainerLogsOptions.stdout]
 * and [ContainerLogsOptions.stderr] options already set.
 *
 * The stream format (multiplexed vs raw TTY) is automatically detected from the
 * content, so there's no need to specify whether the container uses TTY.
 *
 * @param container Container id or name.
 * @return [ContainerLogsResult.StreamDemuxed] containing logs with separated stdout/stderr
 *
 * @throws ContainerNotFoundException If the container is not found.
 */
public suspend fun ContainerResource.logsDemuxed(container: String): ContainerLogsResult.StreamDemuxed =
    logs(
        container = container,
        options =
            ContainerLogsOptions(
                follow = true,
                stderr = true,
                stdout = true,
                demux = true,
            ),
    ) as ContainerLogsResult.StreamDemuxed

/**
 * Get logs from a container as a [Flow] with [ContainerLogsOptions.follow], [ContainerLogsOptions.stdout]
 * and [ContainerLogsOptions.stderr] options already set.
 *
 * The stream format (multiplexed vs raw TTY) is automatically detected from the
 * content, so there's no need to specify whether the container uses TTY.
 *
 * @param container Container id or name.
 * @return [Flow] containing both stdout and stderr logs
 *
 * @throws ContainerNotFoundException If the container is not found.
 */
public suspend fun ContainerResource.logsAsFlow(container: String): Flow<Frame> {
    val options =
        ContainerLogsOptions(
            follow = true,
            stderr = true,
            stdout = true,
            demux = false,
        )
    val result =
        logs(
            container = container,
            options = options,
        ) as ContainerLogsResult.Stream

    return result.output
}

/**
 * Copy files or folders from the local filesystem to a container.
 *
 * This method uploads a tar archive to a container and extracts it
 * at the specified destination path.
 *
 * @param container Container id or name.
 * @param destinationPath Path inside the container where files will be extracted.
 * @param tarArchive The tar archive containing files to copy.
 * @param options Additional options for the copy operation.
 * @throws ContainerNotFoundException If the container is not found.
 * @throws IllegalArgumentException If the destination path is invalid.
 */
public suspend fun ContainerResource.copyTo(
    container: String,
    destinationPath: String,
    tarArchive: ByteArray,
    options: ContainerCopyOptions.() -> Unit,
): Unit =
    copyTo(
        container = container,
        destinationPath = destinationPath,
        tarArchive = tarArchive,
        options = ContainerCopyOptions(path = destinationPath).apply(options),
    )

/**
 * Copy a single file from the local filesystem to a container.
 *
 * This is a convenience method that creates a tar archive from a single file
 * and uploads it to the container.
 *
 * @param container Container id or name.
 * @param sourcePath Path to the file on the local filesystem.
 * @param destinationPath Path inside the container where the file will be copied.
 * @param options Additional options for the copy operation.
 * @throws ArchiveNotFoundException If the source file does not exist.
 * @throws ContainerNotFoundException If the container is not found.
 */
public suspend fun ContainerResource.copyFileTo(
    container: String,
    sourcePath: String,
    destinationPath: String,
    options: ContainerCopyOptions.() -> Unit,
): Unit =
    copyFileTo(
        container = container,
        sourcePath = sourcePath,
        destinationPath = destinationPath,
        options = ContainerCopyOptions(path = destinationPath).apply(options),
    )

/**
 * Copy a directory from the local filesystem to a container.
 *
 * @param container Container ID or name.
 * @param sourcePath Path to the directory on the local filesystem.
 * @param destinationPath Path inside the container where files will be copied.
 * @param options Additional options for the copy operation.
 * @throws kotlinx.io.files.FileNotFoundException If the source directory does not exist.
 * @throws ContainerNotFoundException If the container is not found.
 */
public suspend fun ContainerResource.copyDirectoryTo(
    container: String,
    sourcePath: String,
    destinationPath: String,
    options: ContainerCopyOptions.() -> Unit,
): Unit =
    copyDirectoryTo(
        container = container,
        sourcePath = sourcePath,
        destinationPath = destinationPath,
        options = ContainerCopyOptions(path = destinationPath).apply(options),
    )

/**
 * Attach to a container via WebSocket for bidirectional communication.
 *
 * This method provides full stdin/stdout/stderr support through WebSocket protocol.
 *
 * @param container Container id or name.
 * @param block Attach options.
 * @return [ContainerAttachWebSocketResult] containing the WebSocket streams and send functions.
 * @throws ContainerNotFoundException If the container is not found.
 * @throws ContainerNotRunningException If the container is not running.
 *
 * @see <a href="https://docs.docker.com/engine/api/v1.47/#operation/ContainerAttachWebsocket">Docker API - Container Attach WebSocket</a>
 */
public suspend fun ContainerResource.attachWebSocket(
    container: String,
    block: ContainerAttachOptions.() -> Unit,
): ContainerAttachWebSocketResult = attachWebSocket(container, ContainerAttachOptions().apply(block))
