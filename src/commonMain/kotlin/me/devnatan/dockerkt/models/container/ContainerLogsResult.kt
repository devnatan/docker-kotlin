package me.devnatan.dockerkt.models.container

import kotlinx.coroutines.flow.Flow
import me.devnatan.dockerkt.models.Frame

/**
 * Result of a [me.devnatan.dockerkt.resource.container.ContainerResource.logs] operation.
 *
 * The result type depends on whether streaming mode is used and
 * whether demultiplexing (separating stdout/stderr) is requested.
 *
 * @see Frame
 * @see me.devnatan.dockerkt.models.Stream
 */
public sealed class ContainerLogsResult {
    /**
     * Streaming result with combined stdout/stderr output.
     *
     * The [output] flow emits [Frame] objects containing log lines
     * as they become available. Each frame includes the stream type
     * (stdout or stderr) and the content.
     *
     * @property output Flow of frames containing log lines.
     */
    public data class Stream(
        val output: Flow<Frame>,
    ) : ContainerLogsResult()

    /**
     * Streaming result with separated stdout and stderr.
     *
     * Use this when you need to process stdout and stderr independently.
     *
     * @property stdout Flow of stdout log lines.
     * @property stderr Flow of stderr log lines.
     */
    public data class StreamDemuxed(
        val stdout: Flow<String>,
        val stderr: Flow<String>,
    ) : ContainerLogsResult()

    /**
     * Complete result with all logs collected.
     *
     * This is returned when follow mode is disabled and all logs
     * are fetched at once.
     *
     * @property output Combined stdout and stderr output as a single string.
     */
    public data class Complete(
        val output: String,
    ) : ContainerLogsResult()

    /**
     * Just like [Complete] but with separated stdout and stderr.
     *
     * @property stdout All stdout output as a single string.
     * @property stderr All stderr output as a single string.
     */
    public data class CompleteDemuxed(
        val stdout: String,
        val stderr: String,
    ) : ContainerLogsResult()
}
