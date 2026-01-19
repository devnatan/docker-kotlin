package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.flow.Flow
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.container.Container
import me.devnatan.dockerkt.models.container.ContainerCopyOptions
import me.devnatan.dockerkt.models.container.ContainerCopyResult
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.container.ContainerListOptions
import me.devnatan.dockerkt.models.container.ContainerLogsOptions
import me.devnatan.dockerkt.models.container.ContainerLogsResult
import me.devnatan.dockerkt.models.container.ContainerPruneFilters
import me.devnatan.dockerkt.models.container.ContainerPruneResult
import me.devnatan.dockerkt.models.container.ContainerRemoveOptions
import me.devnatan.dockerkt.models.container.ContainerSummary
import me.devnatan.dockerkt.models.container.ContainerWaitResult
import me.devnatan.dockerkt.resource.image.ImageNotFoundException
import kotlin.time.Duration

public expect class ContainerResource {
    /**
     * Returns a list of all containers.
     *
     * @param options Options to customize the listing result.
     */
    public suspend fun list(options: ContainerListOptions = ContainerListOptions(all = true)): List<ContainerSummary>

    /**
     * Creates a new container.
     *
     * @param options Options to customize the container creation.
     * @throws ImageNotFoundException If the image specified does not exist or isn't pulled.
     * @throws ContainerAlreadyExistsException If a container with the same name already exists.
     */
    public suspend fun create(options: ContainerCreateOptions): String

    /**
     * Removes a container.
     *
     * @param container The container id to remove.
     * @param options Removal options.
     * @throws ContainerNotFoundException If the container is not found for the specified id.
     * @throws ContainerRemoveConflictException When trying to remove an active container without the `force` option.
     */
    public suspend fun remove(
        container: String,
        options: ContainerRemoveOptions = ContainerRemoveOptions(),
    )

    /**
     * Returns low-level information about a container.
     *
     * @param container ID or name of the container.
     * @param size Should return the size of container as fields `SizeRw` and `SizeRootFs`
     */
    public suspend fun inspect(
        container: String,
        size: Boolean = false,
    ): Container

    /**
     * Starts a container.
     *
     * @param container The container id to be started.
     * @param detachKeys The key sequence for detaching a container.
     * @throws ContainerAlreadyStartedException If the container was already started.
     * @throws ContainerNotFoundException If container was not found.
     */
    public suspend fun start(
        container: String,
        detachKeys: String? = null,
    )

    /**
     * Stops a container.
     *
     * @param container The container id to stop.
     * @param timeout Duration to wait before killing the container.
     */
    public suspend fun stop(
        container: String,
        timeout: Duration? = null,
    )

    /**
     * Restarts a container.
     *
     * @param container The container id to restart.
     * @param timeout Duration to wait before killing the container.
     */
    public suspend fun restart(
        container: String,
        timeout: Duration? = null,
    )

    /**
     * Kills a container.
     *
     * @param container The container id to kill.
     * @param signal Signal to send for container to be killed, Docker's default is "SIGKILL".
     */
    public suspend fun kill(
        container: String,
        signal: String? = null,
    )

    /**
     * Renames a container.
     *
     * @param container The container id to rename.
     * @param newName The new container name.
     */
    public suspend fun rename(
        container: String,
        newName: String,
    )

    /**
     * Pauses a container.
     *
     * @param container The container id to pause.
     * @see unpause
     */
    public suspend fun pause(container: String)

    /**
     * Resumes a container which has been paused.
     *
     * @param container The container id to unpause.
     * @see pause
     */
    public suspend fun unpause(container: String)

    /**
     * Resizes the TTY for a container.
     *
     * @param container The container id to resize.
     * @param options Resize options like width and height.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws DockerResponseException If the container cannot be resized or if an error occurs in the request.
     */
    public suspend fun resizeTTY(
        container: String,
        options: ResizeTTYOptions = ResizeTTYOptions(),
    )

    // TODO documentation
    public fun attach(container: String): Flow<Frame>

    // TODO documentation
    public suspend fun wait(
        container: String,
        condition: String? = null,
    ): ContainerWaitResult

    // TODO documentation
    public suspend fun prune(filters: ContainerPruneFilters = ContainerPruneFilters()): ContainerPruneResult

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
     * @param options Configuration options for log retrieval. See [ContainerLogsOptions].
     * @return [ContainerLogsResult] containing logs based on the options:
     *   - [ContainerLogsResult.Stream] for streaming mode
     *   - [ContainerLogsResult.StreamDemuxed] for streaming with separated stdout/stderr
     *   - [ContainerLogsResult.Complete] for non-streaming mode
     *   - [ContainerLogsResult.CompleteDemuxed] for non-streaming with separated stdout/stderr
     *
     * @throws ContainerNotFoundException If the container is not found.
     * @throws IllegalArgumentException If neither stdout nor stderr is enabled.
     */
    public suspend fun logs(
        container: String,
        options: ContainerLogsOptions = ContainerLogsOptions(),
    ): ContainerLogsResult

    /**
     * Copy files or folders from a container to the local filesystem.
     *
     * This method retrieves files from a container as a tar archive.
     * The archive is then extracted to the local filesystem.
     *
     * @param container Container id or name.
     * @param sourcePath Path to the file or folder inside the container.
     * @return [ContainerCopyResult] containing the tar archive and path statistics.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws ArchiveNotFoundException If the path does not exist in the container.
     */
    public suspend fun copyFrom(
        container: String,
        sourcePath: String,
    ): ContainerCopyResult

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
    public suspend fun copyTo(
        container: String,
        destinationPath: String,
        tarArchive: ByteArray,
        options: ContainerCopyOptions = ContainerCopyOptions(path = destinationPath),
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
    public suspend fun copyFileTo(
        container: String,
        sourcePath: String,
        destinationPath: String,
        options: ContainerCopyOptions = ContainerCopyOptions(path = destinationPath),
    )

    /**
     * Copy a file from a container to the local filesystem.
     *
     * This is a convenience method that retrieves a tar archive from the container
     * and extracts a single file from it.
     *
     * @param container Container id or name.
     * @param sourcePath Path to the file inside the container.
     * @param destinationPath Path on the local filesystem where the file will be saved.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws ArchiveNotFoundException If the source path does not exist in the container.
     */
    public suspend fun copyFileFrom(
        container: String,
        sourcePath: String,
        destinationPath: String,
    )

    /**
     * Copy a directory from a container to the local filesystem.
     *
     * @param container Container id or name.
     * @param sourcePath Path to the directory inside the container.
     * @param destinationPath Path on the local filesystem where files will be extracted.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws ArchiveNotFoundException If the source path does not exist in the container.
     */
    public suspend fun copyDirectoryFrom(
        container: String,
        sourcePath: String,
        destinationPath: String,
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
    public suspend fun copyDirectoryTo(
        container: String,
        sourcePath: String,
        destinationPath: String,
        options: ContainerCopyOptions = ContainerCopyOptions(path = destinationPath),
    )
}
