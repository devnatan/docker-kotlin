package me.devnatan.dockerkt.resource.container

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import io.ktor.util.decodeBase64Bytes
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.io.FileSystemUtils
import me.devnatan.dockerkt.io.TarEntry
import me.devnatan.dockerkt.io.TarOperations
import me.devnatan.dockerkt.io.TarUtils
import me.devnatan.dockerkt.io.collectStream
import me.devnatan.dockerkt.io.collectStreamDemuxed
import me.devnatan.dockerkt.io.readStream
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.Stream
import me.devnatan.dockerkt.models.container.Container
import me.devnatan.dockerkt.models.container.ContainerArchiveInfo
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
import kotlin.time.Duration

private const val BasePath = "/containers"

public class ContainerResource internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    /**
     * Returns a list of all containers.
     *
     * @param options Options to customize the listing result.
     */
    public suspend fun list(options: ContainerListOptions = ContainerListOptions(all = true)): List<ContainerSummary> =
        requestCatching {
            httpClient.get("$BasePath/json") {
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
    public suspend fun create(options: ContainerCreateOptions): String {
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
     * Returns low-level information about a container.
     *
     * @param container ID or name of the container.
     * @param size Should return the size of container as fields `SizeRw` and `SizeRootFs`
     */
    public suspend fun inspect(
        container: String,
        size: Boolean = false,
    ): Container =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.get("$BasePath/$container/json") {
                parameter("size", size)
            }
        }.body()

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
     * Stops a container.
     *
     * @param container The container id to stop.
     * @param timeout Duration to wait before killing the container.
     */
    public suspend fun stop(
        container: String,
        timeout: Duration? = null,
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
     * Restarts a container.
     *
     * @param container The container id to restart.
     * @param timeout Duration to wait before killing the container.
     */
    public suspend fun restart(
        container: String,
        timeout: Duration? = null,
    ): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ContainerNotFoundException(exception, container) },
        ) {
            httpClient.post("$BasePath/$container/restart") {
                parameter("t", timeout)
            }
        }

    /**
     * Kills a container.
     *
     * @param container The container id to kill.
     * @param signal Signal to send for container to be killed, Docker's default is "SIGKILL".
     */
    public suspend fun kill(
        container: String,
        signal: String? = null,
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
     * Renames a container.
     *
     * @param container The container id to rename.
     * @param newName The new container name.
     */
    public suspend fun rename(
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
     * Pauses a container.
     *
     * @param container The container id to pause.
     * @see unpause
     */
    public suspend fun pause(container: String): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.post("$BasePath/$container/pause")
        }

    /**
     * Resumes a container which has been paused.
     *
     * @param container The container id to unpause.
     * @see pause
     */
    public suspend fun unpause(container: String): Unit =
        requestCatching(
            HttpStatusCode.NotFound to { cause -> ContainerNotFoundException(cause, container) },
        ) {
            httpClient.post("$BasePath/$container/unpause")
        }

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

    // TODO documentation
    public fun attach(container: String): Flow<Frame> =
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

    // TODO documentation
    public suspend fun wait(
        container: String,
        condition: String? = null,
    ): ContainerWaitResult =
        httpClient
            .post("$BasePath/$container/wait") {
                parameter("condition", condition)
            }.body()

    // TODO documentation
    public suspend fun prune(filters: ContainerPruneFilters = ContainerPruneFilters()): ContainerPruneResult =
        httpClient
            .post("$BasePath/prune") {
                parameter("filters", json.encodeToString(filters))
            }.body()

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
     */
    public suspend fun logs(
        container: String,
        options: ContainerLogsOptions = ContainerLogsOptions(),
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
            val multiplexed = response.headers["Content-Type"] == "application/vnd.docker.multiplexed-stream"

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
                            val multiplexed =
                                httpResponse.headers["Content-Type"] == "application/vnd.docker.multiplexed-stream"

                            readStream(channel, multiplexed) { frame ->
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
    ) {
        val result = copyFrom(container, sourcePath)
        TarOperations.extractTar(result.archiveData, Path(destinationPath))
    }

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
    ) {
        val result = copyFrom(container, sourcePath)
        TarOperations.extractTar(result.archiveData, Path(destinationPath))
    }

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
    ) {
        val path = Path(sourcePath)
        if (!FileSystemUtils.exists(path) || !FileSystemUtils.isDirectory(path)) {
            throw IllegalArgumentException("Source directory not found: $sourcePath")
        }

        val entries = mutableListOf<TarEntry>()
        TarOperations.collectDirectoryContents(path, "", entries)
        val tarArchive = TarUtils.createTarArchive(entries)
        copyTo(container, destinationPath, tarArchive, options)
    }
}
