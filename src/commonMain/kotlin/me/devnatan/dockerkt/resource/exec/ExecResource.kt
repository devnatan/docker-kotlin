package me.devnatan.dockerkt.resource.exec

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readFully
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.IdOnlyResponse
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.Stream
import me.devnatan.dockerkt.models.exec.DemuxedOutput
import me.devnatan.dockerkt.models.exec.ExecCreateOptions
import me.devnatan.dockerkt.models.exec.ExecInspectResponse
import me.devnatan.dockerkt.models.exec.ExecStartOptions
import me.devnatan.dockerkt.models.exec.ExecStartResult
import me.devnatan.dockerkt.resource.ResourcePaths.CONTAINERS
import me.devnatan.dockerkt.resource.container.ContainerNotFoundException
import me.devnatan.dockerkt.resource.container.ContainerNotRunningException
import kotlin.jvm.JvmSynthetic

public const val DefaultBufferSize: Int = 8 * 1024 // 8192 bytes
private const val BasePath: String = "/exec"

/**
 * Exec runs new commands inside running containers.
 *
 * This resource is equivalent to calling `docker exec` on CLI.
 *
 * [Exec Command-line reference](https://docs.docker.com/engine/reference/commandline/exec/)
 */
public class ExecResource internal constructor(
    private val httpClient: HttpClient,
) {
    /**
     * Runs a command inside a running container.
     *
     * @param id The container id to execute the command.
     * @param options Exec instance command options.
     * @throws ContainerNotFoundException If container instance is not found.
     * @throws ContainerNotRunningException If the container is not running.
     */
    @JvmSynthetic
    public suspend fun create(
        id: String,
        options: ExecCreateOptions,
    ): String =
        requestCatching(
            HttpStatusCode.NotFound to { cause ->
                ContainerNotFoundException(
                    cause,
                    id,
                )
            },
            HttpStatusCode.Conflict to { cause ->
                ContainerNotRunningException(
                    cause,
                    id,
                )
            },
        ) {
            httpClient.post("$CONTAINERS/$id/exec") {
                setBody(options)
            }
        }.body<IdOnlyResponse>().id

    /**
     * Inspects an exec instance and returns low-level information about it.
     * `docker exec inspect`
     *
     * @param id Exec instance unique identifier.
     * @throws ExecNotFoundException If exec instance is not found.
     */
    public suspend fun inspect(id: String): ExecInspectResponse =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ExecNotFoundException(exception, id) },
        ) {
            httpClient.get("$BasePath/$id/json")
        }.body()

    /**
     * Starts a previously set up exec instance.
     *
     * This method provides different modes of execution based on the options provided:
     * - **Detached mode** ([ExecStartOptions.detach] = true): Returns immediately after starting the command
     * - **Socket mode** ([ExecStartOptions.socket] = true): Returns a raw connection socket for custom read/write operations
     * - **Stream mode** ([ExecStartOptions.stream] = true): Returns output progressively as a Flow of chunks
     * - **Standard mode**: Collects and returns all output as a single string
     *
     * When [ExecStartOptions.demux] is true and [ExecStartOptions.tty] is false, stdout and stderr are separated.
     *
     * @param id Exec instance unique identifier.
     * @param options Configuration options for starting the exec instance. See [ExecStartOptions] for details.
     * @return [ExecStartResult] containing the output based on the options provided:
     *   - [ExecStartResult.Detached] if detach mode is enabled
     *   - [ExecStartResult.Socket] if socket mode is enabled (must be closed by caller)
     *   - [ExecStartResult.Stream] if stream mode is enabled without demux
     *   - [ExecStartResult.StreamDemuxed] if stream mode is enabled with demux
     *   - [ExecStartResult.Complete] if standard mode without demux
     *   - [ExecStartResult.CompleteDemuxed] if standard mode with demux
     *
     * @throws ExecNotFoundException If exec instance is not found.
     * @throws ContainerNotRunningException If the container in which the exec instance was created is not running.
     */
    public suspend fun start(
        id: String,
        options: ExecStartOptions,
    ): ExecStartResult =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ExecNotFoundException(exception, id) },
            HttpStatusCode.Conflict to { exception -> ContainerNotRunningException(exception, null) },
        ) {
            val response =
                httpClient.post("$BasePath/$id/start") {
                    if (!headers.contains("Connection", "upgrade")) {
                        setBody(options)
                    }
                }

            when {
                options.detach == true -> {
                    ExecStartResult.Detached
                }

                options.socket -> {
                    ExecStartResult.Socket(response.bodyAsChannel())
                }

                options.stream -> {
                    val flow =
                        readFromSocket(
                            channel = response.bodyAsChannel(),
                            tty = options.tty == true,
                            demux = options.demux,
                        )

                    @Suppress("UNCHECKED_CAST")
                    if (options.demux) {
                        ExecStartResult.StreamDemuxed(flow as Flow<DemuxedOutput>)
                    } else {
                        ExecStartResult.Stream(flow as Flow<String>)
                    }
                }

                else -> {
                    val output =
                        collectFromSocket(
                            channel = response.bodyAsChannel(),
                            tty = options.tty == true,
                            demux = options.demux,
                        )
                    if (options.demux) {
                        ExecStartResult.CompleteDemuxed(output as DemuxedOutput)
                    } else {
                        ExecStartResult.Complete(output as String)
                    }
                }
            }
        }

    private fun readFromSocket(
        channel: ByteReadChannel,
        tty: Boolean,
        demux: Boolean,
    ): Flow<*> =
        flow {
            try {
                if (tty) {
                    val buffer = ByteArray(DefaultBufferSize)
                    while (true) {
                        val bytesRead =
                            try {
                                channel.readAvailable(buffer, 0, buffer.size)
                            } catch (e: Exception) {
                                break
                            }

                        if (bytesRead == -1) {
                            break
                        }

                        if (bytesRead > 0) {
                            emit(buffer.copyOf(bytesRead).decodeToString())
                        }
                    }
                } else {
                    while (true) {
                        val header = ByteArray(8)
                        val headerBytesRead =
                            try {
                                channel.readFully(header, 0, 8)
                                8
                            } catch (e: Exception) {
                                break
                            }

                        if (headerBytesRead < 8) {
                            break
                        }

                        val streamType = Stream.typeOfOrNull(header[0])!!
                        val size =
                            ((header[4].toInt() and 0xFF) shl 24) or
                                ((header[5].toInt() and 0xFF) shl 16) or
                                ((header[6].toInt() and 0xFF) shl 8) or
                                (header[7].toInt() and 0xFF)

                        if (size > 0) {
                            val data = ByteArray(size)
                            try {
                                channel.readFully(data, 0, size)
                            } catch (_: Exception) {
                                break
                            }

                            if (demux) {
                                emit(
                                    DemuxedOutput(
                                        stdout = if (streamType == Stream.StdOut) data.decodeToString() else "",
                                        stderr = if (streamType == Stream.StdErr) data.decodeToString() else "",
                                    ),
                                )
                            } else {
                                emit(data.decodeToString())
                            }
                        }
                    }
                }
            } finally {
                channel.cancel()
            }
        }

    private suspend fun collectFromSocket(
        channel: ByteReadChannel,
        tty: Boolean,
        demux: Boolean,
    ): Any =
        try {
            if (demux && !tty) {
                val stdout = StringBuilder()
                val stderr = StringBuilder()

                readFromSocket(channel, tty, demux).collect { output ->
                    output as DemuxedOutput
                    stdout.append(output.stdout)
                    stderr.append(output.stderr)
                }

                DemuxedOutput(stdout.toString(), stderr.toString())
            } else {
                val output = StringBuilder()
                readFromSocket(channel, tty, demux).collect { chunk ->
                    output.append(chunk as String)
                }
                output.toString()
            }
        } finally {
            channel.cancel()
        }

    /**
     * Resizes a TTY session used by an exec instance.
     *
     * Note: Will work only if `tty` option was specified during creation and start of the exec instance.
     *
     * @param id Exec unique identifier.
     * @param options Options for TTY resizing.
     * @throws ExecNotFoundException If exec instance is not found.
     */
    public suspend fun resize(
        id: String,
        options: ResizeTTYOptions,
    ) {
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ExecNotFoundException(exception, id) },
        ) {
            httpClient.post("$BasePath/$id/resize") {
                setBody(options)
            }
        }
    }
}
