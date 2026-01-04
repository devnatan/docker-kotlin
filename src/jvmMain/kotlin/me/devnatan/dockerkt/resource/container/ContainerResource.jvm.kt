package me.devnatan.dockerkt.resource.container

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttpEngine
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.decodeBase64Bytes
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.Buffer
import io.ktor.utils.io.core.discard
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.jvm.nio.toByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.streams.asByteWriteChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.DocketClientConfig
import me.devnatan.dockerkt.io.FileSystemUtils
import me.devnatan.dockerkt.io.HttpSocketPrefix
import me.devnatan.dockerkt.io.TarEntry
import me.devnatan.dockerkt.io.TarOperations
import me.devnatan.dockerkt.io.TarUtils
import me.devnatan.dockerkt.io.TcpSocketPrefix
import me.devnatan.dockerkt.io.UnixSocketPrefix
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.Stream
import me.devnatan.dockerkt.models.container.Container
import me.devnatan.dockerkt.models.container.ContainerArchiveInfo
import me.devnatan.dockerkt.models.container.ContainerAttachOptions
import me.devnatan.dockerkt.models.container.ContainerAttachResult
import me.devnatan.dockerkt.models.container.ContainerCopyOptions
import me.devnatan.dockerkt.models.container.ContainerCopyResult
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.connection.Exchange
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.buffer
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public actual class ContainerResource(
    private val coroutineScope: CoroutineScope,
    private val json: Json,
    private val httpClient: HttpClient,
    private val config: DocketClientConfig,
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

    public actual suspend fun attach(
        container: String,
        options: ContainerAttachOptions,
    ): ContainerAttachResult =
        when {
            options.stdin -> attachSocket(container, options)
            else -> attachDetached(container, options)
        }

    private suspend fun attachDetached(
        container: String,
        options: ContainerAttachOptions,
    ): ContainerAttachResult =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ContainerNotFoundException(exception, container) },
            HttpStatusCode.BadRequest to { exception ->
                IllegalArgumentException(
                    "Invalid attach options: $options",
                    exception,
                )
            },
        ) {
            httpClient.post("$CONTAINERS/$container/attach") {
                parameter("stream", options.stream)
                parameter("stdin", options.stdin)
                parameter("stdout", options.stdout)
                parameter("stderr", options.stderr)
                parameter("logs", options.logs)
                parameter("detachKeys", options.detachKeys)
            }

            ContainerAttachResult.Detached
        }

    private suspend fun attachSocket(
        container: String,
        options: ContainerAttachOptions,
    ): ContainerAttachResult {
        val engine = httpClient.engine as OkHttpEngine
        val client = engine.config.preconfigured!!
        val socketPath = config.socketPath
        val baseUrl =
            when {
                socketPath.startsWith(UnixSocketPrefix) -> "http://localhost"
                socketPath.startsWith(TcpSocketPrefix) -> socketPath.replace(TcpSocketPrefix, "http://")
                socketPath.startsWith(HttpSocketPrefix) -> socketPath
                else -> error("Invalid socket path: $socketPath")
            }

        val requestUrl =
            "$baseUrl$CONTAINERS/$container/attach?stdin=${options.stdin}&stdout=${options.stdout}&stderr=${options.stderr}&logs=${options.logs}&stream=${options.stream}"

        val request =
            Request
                .Builder()
                .url(requestUrl)
                .post(ByteArray(0).toRequestBody(null, 0, 0))
                .header("Connection", "Upgrade")
                .header("Upgrade", "tcp")
                .build()

        val response =
            withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

        val socket = response.socket ?: error("No socket found in response")
        val connection =
            AttachConnection(
                source = socket.source,
                sink = socket.sink,
            )

        val readChannel = connection.source.buffer().toByteReadChannel()

        return ContainerAttachResult.Stream(
            output = readStream(readChannel),
            input = { bytes ->
                val buffer = okio.Buffer()
                buffer.write(bytes)
                socket.sink.write(buffer, buffer.size)
                socket.sink.flush()
            },
        )
    }

    private data class AttachConnection(
        val source: Source,
        val sink: Sink,
    )

    private fun readStream(channel: ByteReadChannel): Flow<Frame> =
        flow {
            try {
                val buffer = ByteArray(1024)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead == -1) break

                    if (bytesRead > 0) {
                        val text = buffer.copyOf(bytesRead).decodeToString()
                        emit(Frame(text, text.length, Stream.StdOut))
                    } else {
                        delay(10)
                    }
                }
            } finally {
                channel.cancel()
            }
        }

    private fun readMultiplexedStream(channel: ByteReadChannel): Flow<Frame> =
        flow {
            try {
                while (!channel.isClosedForRead) {
                    if (channel.availableForRead < 8) {
                        delay(10)
                        continue
                    }

                    val header = channel.readByteArray(8)
                    val stream = Stream.typeOfOrNull(header[0]) ?: error("Invalid stream type: ${header[0].toInt()}")
                    val frameSize =
                        (
                            (
                                ((header[4].toInt() and 0xFF) shl 24) or
                                    (header[5].toInt() and 0xFF)
                            ) shl 16
                        ) or
                            (header[6].toInt() and 0xFF shl 8) or
                            (header[7].toInt() and 0xFF)

                    if (frameSize > 0) {
                        while (!channel.isClosedForRead && channel.availableForRead < frameSize) {
                            delay(10)
                        }

                        val data = ByteArray(frameSize)

                        try {
                            channel.readFully(data)
                        } catch (_: Throwable) {
                            break
                        }

                        emit(Frame(data.decodeToString(), frameSize, stream))
                    }
                }
            } finally {
                channel.cancel()
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

    public actual suspend fun copyFrom(
        container: String,
        sourcePath: String,
    ): ContainerCopyResult =
        requestCatching(
            HttpStatusCode.NotFound to { exception ->
                if (exception.message?.contains("file") == true) {
                    ArchiveNotFoundException(exception, container, sourcePath)
                } else {
                    ContainerNotFoundException(exception, container)
                }
            },
        ) {
            val response =
                httpClient.get("$CONTAINERS/$container/archive") {
                    parameter("path", sourcePath)
                }

            val archiveData = response.readRawBytes()

            val statHeader = response.headers["X-Docker-Container-Path-Stat"]
            val stat =
                statHeader?.let { header ->
                    val decoded = header.decodeBase64Bytes()
                    json.decodeFromString<ContainerArchiveInfo>(decoded.decodeToString())
                }

            ContainerCopyResult(archiveData, stat)
        }

    public actual suspend fun copyTo(
        container: String,
        destinationPath: String,
        tarArchive: ByteArray,
        options: ContainerCopyOptions,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ContainerNotFoundException(exception, container) },
            HttpStatusCode.BadRequest to { exception ->
                IllegalArgumentException("Invalid destination path: $destinationPath", exception)
            },
        ) {
            httpClient.put("$CONTAINERS/$container/archive") {
                parameter("path", destinationPath)
                parameter("noOverwriteDirNonDir", options.noOverwriteDirNonDir.toString())
                parameter("copyUIDGID", options.copyUIDGID.toString())

                setBody(tarArchive)
                contentType(ContentType.Application.OctetStream)
            }
        }

    public actual suspend fun copyFileFrom(
        container: String,
        sourcePath: String,
        destinationPath: String,
    ) {
        val result = copyFrom(container, sourcePath)
        TarOperations.extractTar(result.archiveData, Path(destinationPath))
    }

    public actual suspend fun copyFileTo(
        container: String,
        sourcePath: String,
        destinationPath: String,
        options: ContainerCopyOptions,
    ) {
        val path = Path(sourcePath)
        if (!FileSystemUtils.exists(path)) {
            throw IllegalArgumentException("Source file not found: $sourcePath")
        }

        if (FileSystemUtils.isDirectory(path)) {
            throw IllegalArgumentException("Source is a directory, use copyDirectoryTo instead: $sourcePath")
        }

        val tarArchive = TarOperations.createTarFromFile(path)
        copyTo(container, destinationPath, tarArchive, options)
    }

    public actual suspend fun copyDirectoryFrom(
        container: String,
        sourcePath: String,
        destinationPath: String,
    ) {
        val result = copyFrom(container, sourcePath)
        TarOperations.extractTar(result.archiveData, Path(destinationPath))
    }

    public actual suspend fun copyDirectoryTo(
        container: String,
        sourcePath: String,
        destinationPath: String,
        options: ContainerCopyOptions,
    ) {
        val path = Path(sourcePath)
        if (!FileSystemUtils.exists(path) || !FileSystemUtils.isDirectory(path)) {
            throw IllegalArgumentException("Source directory not found: $sourcePath")
        }

        // Create tar with directory contents only (not including the root directory name)
        val tarArchive = createTarFromDirectoryContents(path)
        copyTo(container, destinationPath, tarArchive, options)
    }

    private fun createTarFromDirectoryContents(dirPath: Path): ByteArray {
        val entries = mutableListOf<TarEntry>()
        TarOperations.collectDirectoryContents(dirPath, "", entries)
        return TarUtils.createTarArchive(entries)
    }

    public actual fun logs(
        container: String,
        options: ContainerLogsOptions,
    ): Flow<Frame> =
        flow {
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
