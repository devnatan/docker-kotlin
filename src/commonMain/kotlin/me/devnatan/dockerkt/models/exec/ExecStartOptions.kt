package me.devnatan.dockerkt.models.exec

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Options for starting an exec instance.
 *
 * These options control how the exec command is executed and how its output is returned.
 *
 * @property detach If true, detach from the exec command and return immediately after starting.
 *   When enabled, no output will be captured. Default: false
 *
 * @property tty If true, allocate a pseudo-TTY for the exec instance.
 *   This should be enabled for interactive commands that require terminal capabilities.
 *   When enabled, stdout and stderr are combined into a single stream. Default: false
 *
 * @property stream If true, return response data progressively as a Flow of strings,
 *   rather than waiting for the command to complete and returning all output at once.
 *   Useful for long-running commands or when you need to process output as it arrives.
 *   Mutually exclusive with [socket]. Default: false
 *
 * @property socket If true, return the raw connection socket to allow custom read/write operations.
 *   The socket must be explicitly closed by the caller when done.
 *   Useful for bidirectional communication with the exec instance.
 *   Mutually exclusive with [stream]. Default: false
 *
 * @property demux If true, separate stdout and stderr in the returned output.
 *   Only works when [tty] is false (a TTY combines stdout and stderr).
 *   When enabled with [stream]=true, returns [ExecStartResult.StreamDemuxed].
 *   When enabled with [stream]=false, returns [ExecStartResult.CompleteDemuxed].
 *   Default: false
 *
 * @see ExecStartResult
 */
@Serializable
public data class ExecStartOptions(
    @SerialName("Detach") var detach: Boolean? = null,
    @SerialName("Tty") var tty: Boolean? = null,
    @Transient var stream: Boolean = false,
    @Transient var socket: Boolean = false,
    @Transient var demux: Boolean = false,
) {
    init {
        require(!(stream && socket)) {
            "stream and socket options are mutually exclusive"
        }
        require(!demux || tty == false) {
            "demux requires tty to be false (TTY combines stdout and stderr)"
        }
    }
}

@Serializable
public sealed class ExecStartResult {
    /** Detached execution - no output */
    public object Detached : ExecStartResult()

    /** Complete output as string */
    public data class Complete(
        val output: String,
    ) : ExecStartResult()

    /** Complete output with separated stdout/stderr */
    public data class CompleteDemuxed(
        val output: DemuxedOutput,
    ) : ExecStartResult()

    /** Streaming output */
    public data class Stream(
        val output: Flow<String>,
    ) : ExecStartResult()

    /** Streaming output with separated stdout/stderr */
    public data class StreamDemuxed(
        val output: Flow<DemuxedOutput>,
    ) : ExecStartResult()

    /** Raw socket connection */
    public data class Socket(
        val channel: ByteReadChannel,
    ) : ExecStartResult()
}

/**
 * Demultiplexed output with separate stdout and stderr.
 */
public data class DemuxedOutput(
    val stdout: String,
    val stderr: String,
)
