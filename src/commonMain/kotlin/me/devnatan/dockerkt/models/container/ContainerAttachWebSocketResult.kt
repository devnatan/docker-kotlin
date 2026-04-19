package me.devnatan.dockerkt.models.container

import kotlinx.coroutines.flow.Flow
import me.devnatan.dockerkt.models.Frame

/**
 * Result of a WebSocket-based container attach operation.
 *
 * Provides bidirectional communication through WebSocket protocol.
 * This is the preferred method for interactive sessions.
 *
 * @property close Function to close the WebSocket session.
 */
public sealed class ContainerAttachWebSocketResult {
    public abstract val close: suspend () -> Unit

    /**
     * Successfully attached via WebSocket with combined output stream.
     *
     * @property output Flow of frames from stdout/stderr.
     * @property sendText Function to send text to stdin.
     * @property sendBinary Function to send binary data to stdin.
     */
    public data class Connected(
        override val close: suspend () -> Unit,
        val output: Flow<Frame>,
        val sendText: suspend (String) -> Unit,
        val sendBinary: suspend (ByteArray) -> Unit,
    ) : ContainerAttachWebSocketResult()

    /**
     * Successfully attached via WebSocket with demuxed streams.
     *
     * @property stdout Flow of stdout content.
     * @property stderr Flow of stderr content.
     * @property sendText Function to send text to stdin.
     * @property sendBinary Function to send binary data to stdin.
     */
    public data class ConnectedDemuxed(
        override val close: suspend () -> Unit,
        val stdout: Flow<String>,
        val stderr: Flow<String>,
        val sendText: suspend (String) -> Unit,
        val sendBinary: suspend (ByteArray) -> Unit,
    ) : ContainerAttachWebSocketResult()
}
