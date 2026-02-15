package me.devnatan.dockerkt.resource.container

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.util.decodeBase64Bytes
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.readUTF8Line
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.io.FileSystemUtils
import me.devnatan.dockerkt.io.TarEntry
import me.devnatan.dockerkt.io.TarOperations
import me.devnatan.dockerkt.io.TarUtils
import me.devnatan.dockerkt.io.collectStream
import me.devnatan.dockerkt.io.collectStreamDemuxed
import me.devnatan.dockerkt.io.isMultiplexedStream
import me.devnatan.dockerkt.io.readPayloadSize
import me.devnatan.dockerkt.io.readStream
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.Stream
import me.devnatan.dockerkt.models.container.Container
import me.devnatan.dockerkt.models.container.ContainerArchiveInfo
import me.devnatan.dockerkt.models.container.ContainerAttachOptions
import me.devnatan.dockerkt.models.container.ContainerAttachWebSocketResult
import me.devnatan.dockerkt.models.container.ContainerCopyOptions
import me.devnatan.dockerkt.models.container.ContainerCopyResult
import me.devnatan.dockerkt.models.container.ContainerCreateOptions
import me.devnatan.dockerkt.models.container.ContainerCreateResult
import me.devnatan.dockerkt.models.container.ContainerListOptions
import me.devnatan.dockerkt.models.container.ContainerLogsOptions
import me.devnatan.dockerkt.models.container.ContainerLogsResult
import me.devnatan.dockerkt.models.container.ContainerPruneFilters
import me.devnatan.dockerkt.models.container.ContainerPruneResult
import me.devnatan.dockerkt.models.container.ContainerRemoveOptions
import me.devnatan.dockerkt.models.container.ContainerSummary
import me.devnatan.dockerkt.models.container.ContainerWaitResult
import me.devnatan.dockerkt.resource.image.ImageNotFoundException
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public const val BasePath: String = "/containers"

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
            httpClient.get("$BasePath/json") {
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
                httpClient.post("$BasePath/create") {
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
            httpClient.delete("$BasePath/$container") {
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
            httpClient.get("$BasePath/$container/json") {
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
            httpClient.post("$BasePath/$container/start") {
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
            httpClient.post("$BasePath/$container/stop") {
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
            httpClient.post("$BasePath/$container/restart") {
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
            httpClient.post("$BasePath/$container/kill") {
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
            httpClient.post("$BasePath/$container/rename") {
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
            httpClient.post("$BasePath/$container/pause")
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
            httpClient.post("$BasePath/$container/unpause")
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
            httpClient.post("$BasePath/$container/resize") {
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
        channelFlow {
            httpClient
                .preparePost("$BasePath/$container/attach") {
                    parameter("stream", "true")
                    parameter("stdin", "true")
                    parameter("stdout", "true")
                    parameter("stderr", "true")
                }.execute { response ->
                    val channel = response.body<ByteReadChannel>()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break

                        // TODO handle stream type
                        send(Frame(line, line.length, Stream.StdOut))
                    }
                }
        }

    @JvmSynthetic
    public actual suspend fun wait(
        container: String,
        condition: String?,
    ): ContainerWaitResult =
        httpClient
            .post("$BasePath/$container/wait") {
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
            .post("$BasePath/prune") {
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
                httpClient.get("$BasePath/$container/archive") {
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
            httpClient.put("$BasePath/$container/archive") {
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

    public actual suspend fun logs(
        container: String,
        options: ContainerLogsOptions,
    ): ContainerLogsResult =
        if (options.follow ?: false) {
            logsStreaming(container, options)
        } else {
            logsComplete(container, options)
        }

    private suspend fun logsComplete(
        container: String,
        options: ContainerLogsOptions,
    ): ContainerLogsResult =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            val response =
                httpClient.get("$BasePath/$container/logs") {
                    parameter("stdout", options.stdout)
                    parameter("stderr", options.stderr)
                    parameter("timestamps", options.showTimestamps)
                    parameter("since", options.since)
                    parameter("until", options.until)
                    parameter("tail", options.tail)
                    parameter("follow", false)
                }

            val channel = response.bodyAsChannel()
            val multiplexed = response.headers.get("Content-Type") == "application/vnd.docker.multiplexed-stream"

            return if (options.demux) {
                val (stdout, stderr) = collectStreamDemuxed(channel, multiplexed)
                ContainerLogsResult.CompleteDemuxed(stdout, stderr)
            } else {
                val content = collectStream(channel, multiplexed)
                ContainerLogsResult.Complete(content)
            }
        }

    private fun logsStreaming(
        container: String,
        options: ContainerLogsOptions,
    ): ContainerLogsResult {
        val framesFlow =
            channelFlow {
                requestCatching(
                    HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
                ) {
                    httpClient
                        .prepareGet("$BasePath/$container/logs") {
                            parameter("stdout", options.stdout)
                            parameter("stderr", options.stderr)
                            parameter("timestamps", options.showTimestamps)
                            parameter("since", options.since)
                            parameter("until", options.until)
                            parameter("tail", options.tail)
                            parameter("follow", true)
                        }.execute { httpResponse ->
                            val channel = httpResponse.bodyAsChannel()
                            val multiplxed =
                                httpResponse.headers.get("Content-Type") == "application/vnd.docker.multiplexed-stream"

                            readStream(channel, multiplxed) { frame ->
                                send(frame)
                            }
                        }
                }
            }

        return if (options.demux) {
            val stdoutFlow =
                flow {
                    framesFlow.collect { frame ->
                        if (frame.stream == Stream.StdOut) {
                            emit(frame.value)
                        }
                    }
                }
            val stderrFlow =
                flow {
                    framesFlow.collect { frame ->
                        if (frame.stream == Stream.StdErr) {
                            emit(frame.value)
                        }
                    }
                }
            ContainerLogsResult.StreamDemuxed(stdoutFlow, stderrFlow)
        } else {
            ContainerLogsResult.Stream(framesFlow)
        }
    }

    public actual suspend fun attachWebSocket(
        container: String,
        options: ContainerAttachOptions,
    ): ContainerAttachWebSocketResult {
        val outputChannel = Channel<Frame>(Channel.BUFFERED)
        var session: DefaultClientWebSocketSession? = null
        var sessionJob: Job? = null

        try {
            val queryParams =
                buildString {
                    append("stdout=${options.stdout}")
                    append("&stderr=${options.stderr}")
                    append("&stdin=${options.stdin}")
                    append("&stream=${options.stream}")
                    append("&logs=${options.logs}")
                    options.detachKeys?.let { append("&detachKeys=$it") }
                }

            sessionJob =
                CoroutineScope(Dispatchers.IO).launch {
                    httpClient.webSocket(
                        urlString = "$BasePath/$container/attach/ws?$queryParams",
                    ) {
                        session = this

                        try {
                            for (frame in incoming) {
                                when (frame) {
                                    is io.ktor.websocket.Frame.Text -> {
                                        val content = frame.readText()
                                        outputChannel.send(Frame(content, content.length, Stream.StdOut))
                                    }

                                    is io.ktor.websocket.Frame.Binary -> {
                                        val bytes = frame.readBytes()
                                        val header by lazy { bytes.copyOf(8) }
                                        if (bytes.size >= 8 && isMultiplexedStream(header)) {
                                            val streamType = Stream.typeOfOrNull(header[0]) ?: Stream.StdOut
                                            val payloadSize = readPayloadSize(header)
                                            if (bytes.size >= 8 + payloadSize) {
                                                val content = bytes.copyOfRange(8, 8 + payloadSize).decodeToString()
                                                outputChannel.send(Frame(content, payloadSize, streamType))
                                            }
                                        } else {
                                            val raw = bytes.decodeToString()
                                            outputChannel.send(Frame(raw, raw.length, Stream.StdOut))
                                        }
                                    }

                                    is io.ktor.websocket.Frame.Close -> {
                                        break
                                    }

                                    else -> { /* Ignore ping/pong */ }
                                }
                            }
                        } catch (_: ClosedReceiveChannelException) {
                            // Connection closed normally
                        } catch (e: CancellationException) {
                            throw e
                        } finally {
                            outputChannel.close()
                        }
                    }
                }

            var attempts = 0
            while (session == null && attempts < 50) {
                delay(10)
                attempts++
            }

            if (session == null) {
                throw IllegalStateException("Failed to establish WebSocket connection")
            }

            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            val currentSession = session!!

            val sendText: suspend (String) -> Unit = { text ->
                currentSession.send(
                    io.ktor.websocket.Frame
                        .Text(text),
                )
            }

            val sendBinary: suspend (ByteArray) -> Unit = { data ->
                currentSession.send(
                    io.ktor.websocket.Frame
                        .Binary(true, data),
                )
            }

            val closeSession: suspend () -> Unit = {
                try {
                    currentSession.close(CloseReason(CloseReason.Codes.NORMAL, "Detaching"))
                } catch (_: Throwable) {
                    // Ignore close errors
                }

                sessionJob.cancel()
                outputChannel.close()
            }

            val outputFlow = outputChannel.receiveAsFlow()
            val demux = options.stdout && options.stderr && !options.stdin

            return if (demux) {
                val sharedFlow =
                    outputFlow.shareIn(
                        scope = CoroutineScope(Dispatchers.Default),
                        started = SharingStarted.Lazily,
                    )

                ContainerAttachWebSocketResult.ConnectedDemuxed(
                    stdout =
                        sharedFlow
                            .filter { frame -> frame.stream == Stream.StdOut }
                            .map { frame -> frame.value },
                    stderr =
                        sharedFlow
                            .filter { frame -> frame.stream == Stream.StdErr }
                            .map { frame -> frame.value },
                    sendText = sendText,
                    sendBinary = sendBinary,
                    close = closeSession,
                )
            } else {
                ContainerAttachWebSocketResult.Connected(
                    output = outputFlow,
                    sendText = sendText,
                    sendBinary = sendBinary,
                    close = closeSession,
                )
            }
        } catch (e: Throwable) {
            outputChannel.close()
            sessionJob?.cancel()
            throw e
        }
    }
}
