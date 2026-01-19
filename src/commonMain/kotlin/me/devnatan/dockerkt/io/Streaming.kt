package me.devnatan.dockerkt.io

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readFully
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.Stream

private const val DefaultBufferSize = 8192
private const val HeaderSize = 8

internal suspend fun readStream(
    channel: ByteReadChannel,
    multiplexed: Boolean,
    onFrame: suspend (Frame) -> Unit,
) {
    val header = ByteArray(HeaderSize)

    try {
        channel.readFully(header, 0, HeaderSize)
    } catch (_: Exception) {
        return
    }

    if (multiplexed) {
        val firstFrame = parseFrameFromHeader(header, channel)
        if (firstFrame != null) {
            onFrame(firstFrame)
        }

        // Continue reading remaining frames
        readMultiplexedFrames(channel, onFrame)
    } else {
        // Emit the header bytes as content (they're actual data, not protocol)
        val initialContent = header.decodeToString()
        onFrame(Frame(initialContent, 8, Stream.StdOut))

        // Continue reading raw data
        readRawFrames(channel, onFrame)
    }
}

/**
 * Parses a single frame from an already-read header.
 */
private suspend fun parseFrameFromHeader(
    header: ByteArray,
    channel: ByteReadChannel,
): Frame? {
    val streamType = Stream.typeOfOrNull(header[0]) ?: Stream.StdOut
    val size =
        readPayloadSize(header)

    if (size <= 0) return null

    val data = ByteArray(size)
    try {
        channel.readFully(data, 0, size)
    } catch (_: Exception) {
        return null
    }

    return Frame(value = data.decodeToString(), length = size, stream = streamType)
}

/**
 * Continues reading multiplexed frames after the initial detection.
 */
private suspend fun readMultiplexedFrames(
    channel: ByteReadChannel,
    onFrame: suspend (Frame) -> Unit,
) {
    while (!channel.isClosedForRead) {
        val header = ByteArray(HeaderSize)
        try {
            channel.readFully(header, 0, HeaderSize)
        } catch (_: Exception) {
            break
        }

        val size = readPayloadSize(header)

        if (size > 0) {
            val data = ByteArray(size)
            try {
                channel.readFully(data, 0, size)
            } catch (_: Exception) {
                break
            }
            onFrame(
                Frame(
                    value = data.decodeToString(),
                    length = size,
                    stream = Stream.typeOfOrNull(header[0]) ?: Stream.StdOut,
                ),
            )
        }
    }
}

private fun readPayloadSize(header: ByteArray): Int =
    ((header[4].toInt() and 0xFF) shl 24) or
        ((header[5].toInt() and 0xFF) shl 16) or
        ((header[6].toInt() and 0xFF) shl 8) or
        (header[7].toInt() and 0xFF)

/**
 * Continues reading raw TTY frames after the initial detection.
 */
private suspend fun readRawFrames(
    channel: ByteReadChannel,
    onFrame: suspend (Frame) -> Unit,
) {
    val buffer = ByteArray(DefaultBufferSize)
    while (!channel.isClosedForRead) {
        val bytesRead =
            try {
                channel.readAvailable(buffer, 0, buffer.size)
            } catch (_: Exception) {
                break
            }

        if (bytesRead == -1) break

        if (bytesRead > 0) {
            onFrame(
                Frame(
                    value = buffer.copyOf(bytesRead).decodeToString(),
                    length = bytesRead,
                    stream = Stream.StdOut,
                ),
            )
        }
    }
}

/**
 * Collects all frames into a single string.
 */
internal suspend fun collectStream(
    channel: ByteReadChannel,
    multiplexed: Boolean,
): String {
    val output = StringBuilder()
    readStream(channel, multiplexed) { frame ->
        output.append(frame.value)
    }
    return output.toString()
}

/**
 * Collects frames with demuxing.
 */
internal suspend fun collectStreamDemuxed(
    channel: ByteReadChannel,
    multiplexed: Boolean,
): Pair<String, String> {
    val stdout = StringBuilder()
    val stderr = StringBuilder()

    readStream(channel, multiplexed) { frame ->
        when (frame.stream) {
            Stream.StdOut -> {
                stdout.append(frame.value)
            }

            Stream.StdErr -> {
                stderr.append(frame.value)
            }

            else -> {}
        }
    }

    return stdout.toString() to stderr.toString()
}
