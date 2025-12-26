package me.devnatan.dockerkt.resource.container

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.util.cio.toByteReadChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.core.discard
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.io.RawSource
import kotlinx.io.asInputStream
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.io.readTarFile
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.io.writeTarFile
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.Stream
import me.devnatan.dockerkt.models.container.Container
import me.devnatan.dockerkt.models.container.ContainerArchiveInfo
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.container.ContainerCreateResult
import me.devnatan.dockerkt.models.container.ContainerListOptions
import me.devnatan.dockerkt.models.container.ContainerLogsOptions
import me.devnatan.dockerkt.models.container.ContainerPruneFilters
import me.devnatan.dockerkt.models.container.ContainerPruneResult
import me.devnatan.dockerkt.models.container.ContainerRemoveOptions
import me.devnatan.dockerkt.models.container.ContainerSummary
import me.devnatan.dockerkt.models.container.ContainerWaitResult
import me.devnatan.dockerkt.resource.ResourcePaths.CONTAINERS
import me.devnatan.dockerkt.resource.image.ImageNotFoundException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public actual class ContainerResource(
    private val coroutineScope: CoroutineScope,
    private val json: Json,
    private val httpClient: HttpClient,
) {
    /**
     * Returns a list of all containers.
     *
     * @param options Options to customize the listing result.
     */
    @JvmSynthetic
    public actual suspend fun list(options: ContainerListOptions): List<ContainerSummary> =
        requestCatching {
            httpClient.get("$CONTAINERS/json") {
                parameter("all", options.all)
                parameter("limit", options.limit)
                parameter("size", options.size)
                parameter("filters", options.filters?.let(json::encodeToString))
            }
        }.body()

    /**
     * Returns a list of all containers.
     *
     * @param options Options to customize the listing result.
     */
    @JvmOverloads
    public fun listAsync(options: ContainerListOptions = ContainerListOptions(all = true)): CompletableFuture<List<ContainerSummary>> =
        coroutineScope.async { list(options) }.asCompletableFuture()

    /**
     * Creates a new container.
     *
     * @param options Options to customize the container creation.
     * @throws ImageNotFoundException If the image specified does not exist or isn't pulled.
     * @throws ContainerAlreadyExistsException If a container with the same name already exists.
     */
    @JvmSynthetic
    public actual suspend fun create(options: ContainerCreateOptions): String {
        requireNotNull(options.image) { "Container image is required" }

        val result =
            requestCatching(
                HttpStatusCode.NotFound to { exception -> ImageNotFoundException(exception, options.image.orEmpty()) },
                HttpStatusCode.Conflict to { exception ->
                    ContainerAlreadyExistsException(
                        exception,
                        options.name.orEmpty(),
                    )
                },
            ) {
                httpClient.post("$CONTAINERS/create") {
                    parameter("name", options.name)
                    setBody(options)
                }
            }.body<ContainerCreateResult>()

        result.warnings.forEach { warn -> println("Warning from Docker API: $warn") }
        return result.id
    }

    /**
     * Creates a new container.
     *
     * @param options Options to customize the container creation.
     * @throws ImageNotFoundException If the image specified does not exist or isn't pulled.
     * @throws ContainerAlreadyExistsException If a container with the same name already exists.
     */
    public fun createAsync(options: ContainerCreateOptions): CompletableFuture<String> =
        coroutineScope
            .async {
                create(options)
            }.asCompletableFuture()

    /**
     * Removes a container.
     *
     * @param container The container id to remove.
     * @param options Removal options.
     * @throws ContainerNotFoundException If the container is not found for the specified id.
     * @throws ContainerRemoveConflictException When trying to remove an active container without the `force` option.
     */
    @JvmSynthetic
    public actual suspend fun remove(
        container: String,
        options: ContainerRemoveOptions,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
            HttpStatusCode.Conflict to { cause -> ContainerRemoveConflictException(cause, container) },
        ) {
            httpClient.delete("$CONTAINERS/$container") {
                parameter("v", options.removeAnonymousVolumes)
                parameter("force", options.force)
                parameter("link", options.unlink)
            }
        }

    /**
     * Removes a container.
     *
     * @param container The container id to remove.
     * @param options Removal options.
     * @throws ContainerNotFoundException If the container is not found for the specified id.
     * @throws ContainerRemoveConflictException When trying to remove an active container without the `force` option.
     */
    @JvmOverloads
    public fun removeAsync(
        container: String,
        options: ContainerRemoveOptions = ContainerRemoveOptions(),
    ): CompletableFuture<Unit> = coroutineScope.async { remove(container, options) }.asCompletableFuture()

    /**
     * Returns low-level information about a container.
     *
     * @param container ID or name of the container.
     * @param size Should return the size of container as fields `SizeRw` and `SizeRootFs`
     */
    @JvmSynthetic
    public actual suspend fun inspect(
        container: String,
        size: Boolean,
    ): Container =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.get("$CONTAINERS/$container/json") {
                parameter("size", size)
            }
        }.body()

    /**
     * Returns low-level information about a container.
     *
     * @param container ID or name of the container.
     * @param size Should return the size of container as fields `SizeRw` and `SizeRootFs`
     */
    @JvmOverloads
    public fun inspectAsync(
        container: String,
        size: Boolean = false,
    ): CompletableFuture<Container> = coroutineScope.async { inspect(container, size) }.asCompletableFuture()

    /**
     * Starts a container.
     *
     * @param container The container id to be started.
     * @param detachKeys The key sequence for detaching a container.
     * @throws ContainerAlreadyStartedException If the container was already started.
     * @throws ContainerNotFoundException If container was not found.
     */
    @JvmSynthetic
    public actual suspend fun start(
        container: String,
        detachKeys: String?,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotModified to { cause -> ContainerAlreadyStartedException(cause, container) },
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.post("$CONTAINERS/$container/start") {
                parameter("detachKeys", detachKeys)
            }
        }

    /**
     * Starts a container.
     *
     * @param id The container id to be started.
     * @param detachKeys The key sequence for detaching a container.
     * @throws ContainerAlreadyStartedException If the container was already started.
     * @throws ContainerNotFoundException If container was not found.
     */
    @JvmOverloads
    public fun startAsync(
        id: String,
        detachKeys: String? = null,
    ): CompletableFuture<Unit> =
        coroutineScope
            .async {
                start(id, detachKeys)
            }.asCompletableFuture()

    /**
     * Stops a container.
     *
     * @param container The container id to stop.
     * @param timeout Duration to wait before killing the container.
     */
    @JvmSynthetic
    public actual suspend fun stop(
        container: String,
        timeout: Duration?,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotModified to { cause -> ContainerAlreadyStoppedException(cause, container) },
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.post("$CONTAINERS/$container/stop") {
                parameter("t", timeout?.inWholeSeconds)
            }
        }

    /**
     * Stops a container.
     *
     * @param container The container id to stop.
     */
    public fun stopAsync(container: String): CompletableFuture<Unit> =
        coroutineScope
            .async {
                stop(container, timeout = null)
            }.asCompletableFuture()

    /**
     * Stops a container.
     *
     * @param container The container id to stop.
     * @param timeoutInSeconds Duration in seconds to wait before killing the container.
     */
    public fun stopAsync(
        container: String,
        timeoutInSeconds: Int,
    ): CompletableFuture<Unit> =
        coroutineScope
            .async {
                stop(container, timeoutInSeconds.toDuration(DurationUnit.SECONDS))
            }.asCompletableFuture()

    /**
     * Restarts a container.
     *
     * @param container The container id to restart.
     * @param timeout Duration to wait before killing the container.
     */
    @JvmSynthetic
    public actual suspend fun restart(
        container: String,
        timeout: Duration?,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ContainerNotFoundException(exception, container) },
        ) {
            httpClient.post("$CONTAINERS/$container/restart") {
                parameter("t", timeout)
            }
        }

    /**
     * Restarts a container.
     *
     * @param container The container id to restart.
     */
    public fun restartAsync(container: String): CompletableFuture<Unit> =
        coroutineScope
            .async {
                restart(container, timeout = null)
            }.asCompletableFuture()

    /**
     * Restarts a container.
     *
     * @param container The container id to restart.
     * @param timeoutInSeconds Duration in seconds to wait before killing the container.
     */
    public fun restartAsync(
        container: String,
        timeoutInSeconds: Int,
    ): CompletableFuture<Unit> =
        coroutineScope
            .async {
                restart(container, timeoutInSeconds.toDuration(DurationUnit.SECONDS))
            }.asCompletableFuture()

    /**
     * Kills a container.
     *
     * @param container The container id to kill.
     * @param signal Signal to send for container to be killed, Docker's default is "SIGKILL".
     */
    @JvmSynthetic
    public actual suspend fun kill(
        container: String,
        signal: String?,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
            HttpStatusCode.Conflict to { cause -> ContainerNotRunningException(cause, container) },
        ) {
            httpClient.post("$CONTAINERS/$container/kill") {
                parameter("signal", signal)
            }
        }

    /**
     * Kills a container.
     *
     * @param container The container id to kill.
     * @param signal Signal to send for container to be killed, Docker's default is "SIGKILL".
     */
    @JvmOverloads
    public fun killAsync(
        container: String,
        signal: String? = null,
    ): CompletableFuture<Unit> =
        coroutineScope
            .async {
                kill(container, signal)
            }.asCompletableFuture()

    /**
     * Renames a container.
     *
     * @param container The container id to rename.
     * @param newName The new container name.
     */
    @JvmSynthetic
    public actual suspend fun rename(
        container: String,
        newName: String,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
            HttpStatusCode.Conflict to { cause -> ContainerRenameConflictException(cause, container, newName) },
        ) {
            httpClient.post("$CONTAINERS/$container/rename") {
                parameter("name", newName)
            }
        }

    /**
     * Renames a container.
     *
     * @param container The container id to rename.
     * @param newName The new container name.
     */
    public fun renameAsync(
        container: String,
        newName: String,
    ): CompletableFuture<Unit> =
        coroutineScope
            .async {
                rename(container, newName)
            }.asCompletableFuture()

    /**
     * Pauses a container.
     *
     * @param container The container id to pause.
     * @see unpause
     */
    @JvmSynthetic
    public actual suspend fun pause(container: String): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.post("$CONTAINERS/$container/pause")
        }

    /**
     * Pauses a container.
     *
     * @param container The container id to pause.
     * @see unpause
     */
    public fun pauseAsync(container: String): CompletableFuture<Unit> = coroutineScope.async { pause(container) }.asCompletableFuture()

    /**
     * Resumes a container which has been paused.
     *
     * @param container The container id to unpause.
     * @see pause
     */
    @JvmSynthetic
    public actual suspend fun unpause(container: String): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.post("$CONTAINERS/$container/unpause")
        }

    /**
     * Resumes a container which has been paused.
     *
     * @param container The container id to unpause.
     * @see pause
     */
    public fun unpauseAsync(container: String): CompletableFuture<Unit> = coroutineScope.async { unpause(container) }.asCompletableFuture()

    /**
     * Resizes the TTY for a container.
     *
     * @param container The container id to resize.
     * @param options Resize options like width and height.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws DockerResponseException If the container cannot be resized or if an error occurs in the request.
     */
    @JvmSynthetic
    public actual suspend fun resizeTTY(
        container: String,
        options: ResizeTTYOptions,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause ->
                ContainerNotFoundException(
                    cause,
                    container,
                )
            },
        ) {
            httpClient.post("$CONTAINERS/$container/resize") {
                setBody(options)
            }
        }

    /**
     * Resizes the TTY for a container.
     *
     * @param container Unique identifier or name of the container.
     * @param options Resize options like width and height.
     * @throws ContainerNotFoundException If the container is not found.
     * @throws DockerResponseException If the container cannot be resized or if an error occurs in the request.
     */
    @JvmOverloads
    public fun resizeTTYAsync(
        container: String,
        options: ResizeTTYOptions = ResizeTTYOptions(),
    ): CompletableFuture<Unit> = coroutineScope.async { resizeTTY(container, options) }.asCompletableFuture()

    @JvmSynthetic
    public actual fun attach(container: String): Flow<Frame> =
        flow {
            httpClient
                .preparePost("$CONTAINERS/$container/attach") {
                    parameter("stream", "true")
                    parameter("stdin", "true")
                    parameter("stdout", "true")
                    parameter("stderr", "true")
                }.execute { response ->
                    val channel = response.body<ByteReadChannel>()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break

                        // TODO handle stream type
                        emit(Frame(line, line.length, Stream.StdOut))
                    }
                }
        }

    @JvmSynthetic
    public actual suspend fun wait(
        container: String,
        condition: String?,
    ): ContainerWaitResult =
        httpClient
            .post("$CONTAINERS/$container/wait") {
                parameter("condition", condition)
            }.body()

    @JvmOverloads
    public fun waitAsync(
        id: String,
        condition: String? = null,
    ): CompletableFuture<ContainerWaitResult> = coroutineScope.async { wait(id, condition) }.asCompletableFuture()

    @JvmSynthetic
    public actual suspend fun prune(filters: ContainerPruneFilters): ContainerPruneResult =
        httpClient
            .post("$CONTAINERS/prune") {
                parameter("filters", json.encodeToString(filters))
            }.body()

    @JvmOverloads
    public fun pruneAsync(filters: ContainerPruneFilters = ContainerPruneFilters()): CompletableFuture<ContainerPruneResult> =
        coroutineScope.async { prune(filters) }.asCompletableFuture()

    /**
     * Retrieves information about files of a container file system.
     *
     * @param container The container id.
     * @param path The path to the file or directory inside the container file system.
     */
    @OptIn(ExperimentalEncodingApi::class)
    public actual suspend fun archive(
        container: String,
        path: String,
    ): ContainerArchiveInfo =
        requestCatching {
            val response =
                httpClient.head("$CONTAINERS/$container/archive") {
                    parameter("path", path)
                }

            val pathStat = response.headers["X-Docker-Container-Path-Stat"] ?: error("Missing path stat header")
            val decoded = Base64.decode(pathStat).decodeToString()
            return json.decodeFromString(decoded)
        }

    /**
     * Downloads files from a container file system.
     *
     * @param container The container id.
     * @param remotePath The path to the file or directory inside the container file system.
     */
    public actual suspend fun downloadArchive(
        container: String,
        remotePath: String,
    ): RawSource {
        val contents =
            requestCatching {
                httpClient.get("$CONTAINERS/$container/archive") {
                    accept(ContentType.parse("application/x-tar"))
                    parameter("path", remotePath)
                }
            }.body<InputStream>()
        return readTarFile(contents.asSource())
    }

    /**
     * Uploads files into a container file system.
     *
     * @param container The container id.
     * @param inputPath Path to the file that will be uploaded.
     * @param remotePath Path to the file or directory inside the container file system.
     */
    public actual suspend fun uploadArchive(
        container: String,
        inputPath: String,
        remotePath: String,
    ): Unit =
        requestCatching {
            val archive = writeTarFile(inputPath)

            httpClient.put("$CONTAINERS/$container/archive") {
                parameter("path", remotePath.ifEmpty { FS_ROOT })
                parameter("noOverwriteDirNonDir", false)
                setBody(archive.buffered().asInputStream().toByteReadChannel())
            }
        }

    public actual fun logs(
        container: String,
        options: ContainerLogsOptions,
    ): Flow<Frame> =
        flow {
            requestCatching(HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) }) {
                httpClient
                    .prepareGet("$CONTAINERS/$container/logs") {
                        parameter("follow", options.follow)
                        parameter("stdout", options.stdout)
                        parameter("stderr", options.stderr)
                        parameter("since", options.since)
                        parameter("until", options.until)
                        parameter("timestamps", options.showTimestamps)
                        parameter("tail", options.tail)
                    }.execute { response ->
                        val channel = response.body<ByteReadChannel>()
                        if (channel.isClosedForRead) {
                            throw CancellationException("Container stopped? Logs are not available.")
                        }

                        while (!channel.isClosedForRead) {
                            val fb: Byte =
                                runCatching {
                                    channel.readByte()
                                }.getOrElse {
                                    break // container stopped while streaming logs?
                                }

                            val stream = Stream.typeOfOrNull(fb)

                            // Unknown stream = tty enabled
                            if (stream == null) {
                                val remaining = channel.availableForRead

                                // Remaining +1 includes the previously read first byte. Reinsert the first byte since we read it
                                // before but the type was not expected, so this byte is actually the first character of the line.
                                val len = remaining + 1
                                val payload =
                                    ByteReadChannel(
                                        ByteArray(len) {
                                            if (it == 0) fb else channel.readByte()
                                        },
                                    )

                                val line = payload.readUTF8Line() ?: error("Payload cannot be null")

                                // Try to determine the "correct" stream since we cannot have this information.
                                val stdoutEnabled = options.stdout ?: false
                                val stdErrEnabled = options.stderr ?: false
                                val expectedStream: Stream =
                                    stream ?: when {
                                        stdoutEnabled && !stdErrEnabled -> Stream.StdOut
                                        stdErrEnabled && !stdoutEnabled -> Stream.StdErr
                                        else -> Stream.Unknown
                                    }

                                emit(Frame(line, len, expectedStream))
                                continue
                            }

                            val header = channel.readPacket(7)

                            // We discard the first three bytes because the payload size is in the last four bytes
                            // and the total header size is 8.
                            header.discard(3)

                            val payloadLength = header.readInt()
                            val payloadData = channel.readUTF8Line(payloadLength)!!
                            emit(Frame(payloadData, payloadLength, stream))
                        }
                    }
            }
        }
}
