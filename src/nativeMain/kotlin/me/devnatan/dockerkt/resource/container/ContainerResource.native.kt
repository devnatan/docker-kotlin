package me.devnatan.dockerkt.resource.container

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.io.requestCatching
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
import kotlin.jvm.JvmSynthetic
import kotlin.time.Duration

public actual class ContainerResource(
    public val httpClient: HttpClient,
    public val json: Json,
) {
    /**
     * Returns a list of all containers.
     *
     * @param options Options to customize the listing result.
     */
    public actual suspend fun list(options: ContainerListOptions): List<ContainerSummary> =
        requestCatching {
            httpClient.get("containers/json") {
                parameter("all", options.all)
                parameter("limit", options.limit)
                parameter("size", options.size)
                parameter("filters", options.filters?.let(json::encodeToString))
            }
        }.body()

    /**
     * Creates a new container.
     *
     * @param options Options to customize the container creation.
     * @throws ImageNotFoundException If the image specified does not exist or isn't pulled.
     * @throws ContainerAlreadyExistsException If a container with the same name already exists.
     */
    public actual suspend fun create(options: ContainerCreateOptions): String {
        TODO("Not yet implemented")
    }

    /**
     * Removes a container.
     *
     * @param container The container id to remove.
     * @param options Removal options.
     * @throws ContainerNotFoundException If the container is not found for the specified id.
     * @throws ContainerRemoveConflictException When trying to remove an active container without the `force` option.
     */
    public actual suspend fun remove(
        container: String,
        options: ContainerRemoveOptions,
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Returns low-level information about a container.
     *
     * @param container ID or name of the container.
     * @param size Should return the size of container as fields `SizeRw` and `SizeRootFs`
     */
    public actual suspend fun inspect(
        container: String,
        size: Boolean,
    ): Container {
        TODO("Not yet implemented")
    }

    /**
     * Starts a container.
     *
     * @param container The container id to be started.
     * @param detachKeys The key sequence for detaching a container.
     * @throws ContainerAlreadyStartedException If the container was already started.
     * @throws ContainerNotFoundException If container was not found.
     */
    public actual suspend fun start(
        container: String,
        detachKeys: String?,
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Stops a container.
     *
     * @param container The container id to stop.
     * @param timeout Duration to wait before killing the container.
     */
    public actual suspend fun stop(
        container: String,
        timeout: Duration?,
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Restarts a container.
     *
     * @param container The container id to restart.
     * @param timeout Duration to wait before killing the container.
     */
    public actual suspend fun restart(
        container: String,
        timeout: Duration?,
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Kills a container.
     *
     * @param container The container id to kill.
     * @param signal Signal to send for container to be killed, Docker's default is "SIGKILL".
     */
    public actual suspend fun kill(
        container: String,
        signal: String?,
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Renames a container.
     *
     * @param container The container id to rename.
     * @param newName The new container name.
     */
    public actual suspend fun rename(
        container: String,
        newName: String,
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Pauses a container.
     *
     * @param container The container id to pause.
     * @see unpause
     */
    public actual suspend fun pause(container: String) {
        TODO("Not yet implemented")
    }

    /**
     * Resumes a container which has been paused.
     *
     * @param container The container id to unpause.
     * @see pause
     */
    public actual suspend fun unpause(container: String) {
        TODO("Not yet implemented")
    }

    /**
     * Resizes the TTY for a container.
     *
     * @param container The container id to resize.
     * @param options Resize options like width and height.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws me.devnatan.dockerkt.DockerResourceException If the container cannot be resized or if an error occurs in the request.
     */
    public actual suspend fun resizeTTY(
        container: String,
        options: ResizeTTYOptions,
    ) {
        TODO("Not yet implemented")
    }

    public actual fun attach(container: String): Flow<Frame> {
        TODO("Not yet implemented")
    }

    public actual suspend fun wait(
        container: String,
        condition: String?,
    ): ContainerWaitResult {
        TODO("Not yet implemented")
    }

    public actual suspend fun prune(filters: ContainerPruneFilters): ContainerPruneResult {
        TODO("Not yet implemented")
    }

    public actual suspend fun logs(
        container: String,
        options: ContainerLogsOptions,
    ): ContainerLogsResult {
        TODO("Not yet implemented")
    }

    public actual suspend fun copyFrom(
        container: String,
        sourcePath: String,
    ): ContainerCopyResult {
        TODO("Not yet implemented")
    }

    public actual suspend fun copyTo(
        container: String,
        destinationPath: String,
        tarArchive: ByteArray,
        options: ContainerCopyOptions,
    ) {
    }

    public actual suspend fun copyFileTo(
        container: String,
        sourcePath: String,
        destinationPath: String,
        options: ContainerCopyOptions,
    ) {
    }

    public actual suspend fun copyFileFrom(
        container: String,
        sourcePath: String,
        destinationPath: String,
    ) {
    }

    public actual suspend fun copyDirectoryFrom(
        container: String,
        sourcePath: String,
        destinationPath: String,
    ) {
    }

    public actual suspend fun copyDirectoryTo(
        container: String,
        sourcePath: String,
        destinationPath: String,
        options: ContainerCopyOptions,
    ) {
    }
}
