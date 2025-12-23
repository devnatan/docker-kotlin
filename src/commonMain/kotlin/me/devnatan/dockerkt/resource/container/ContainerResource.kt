package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.RawSource
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.container.Container
import me.devnatan.dockerkt.models.container.ContainerArchiveInfo
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.container.ContainerListOptions
import me.devnatan.dockerkt.models.container.ContainerLogsOptions
import me.devnatan.dockerkt.models.container.ContainerPruneFilters
import me.devnatan.dockerkt.models.container.ContainerPruneResult
import me.devnatan.dockerkt.models.container.ContainerRemoveOptions
import me.devnatan.dockerkt.models.container.ContainerSummary
import me.devnatan.dockerkt.models.container.ContainerWaitResult
import me.devnatan.dockerkt.resource.image.ImageNotFoundException
import kotlin.jvm.JvmOverloads
import kotlin.time.Duration

internal const val FS_ROOT = "/"

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
     * Retrieves information about files of a container file system.
     *
     * @param container The container id.
     * @param path The path to the file or directory inside the container file system.
     */
    @JvmOverloads
    public suspend fun archive(
        container: String,
        path: String = FS_ROOT,
    ): ContainerArchiveInfo

    /**
     * Downloads files from a container file system.
     *
     * @param container The container id.
     * @param remotePath The path to the file or directory inside the container file system.
     */
    public suspend fun downloadArchive(
        container: String,
        remotePath: String,
    ): RawSource

    /**
     * Uploads files into a container file system.
     *
     * @param container The container id.
     * @param inputPath Path to the file that will be uploaded.
     * @param remotePath Path to the file or directory inside the container file system.
     */
    public suspend fun uploadArchive(
        container: String,
        inputPath: String,
        remotePath: String,
    )

    public fun logs(container: String, options: ContainerLogsOptions): Flow<Frame>
}
