package me.devnatan.dockerkt.io

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readFully
import me.devnatan.dockerkt.models.Frame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.devnatan.dockerkt.models.Stream

private const val DefaultBufferSize = 8192

/**
 * Detects if the stream is using Docker's multiplexed protocol or raw TTY output.
 *
 * Docker multiplexed stream format (non-TTY):
 * - Byte 0: Stream type (0=stdin, 1=stdout, 2=stderr)
 * - Bytes 1-3: Reserved, always 0x00
 * - Bytes 4-7: Payload size (big-endian uint32)
 *
 * For TTY-enabled containers, output is raw without this header structure.
 *
 * Detection logic:
 * 1. First byte must be 0, 1, or 2 (valid stream type)
 * 2. Bytes 1-3 must be 0x00 (reserved bytes)
 * 3. Payload size (bytes 4-7) must be reasonable (> 0 and < 10MB)
 *
 * @param header First 8 bytes of the stream
 * @return true if multiplexed protocol detected, false if raw TTY stream
 */
private fun isMultiplexedStream(header: ByteArray): Boolean {
    if (header.size < 8) return false

    // Check stream type (must be 0, 1, or 2)
    val streamType = header[0].toInt() and 0xFF
    if (streamType !in Stream.StdIn.code..Stream.StdErr.code) {
        return false
    }

    // Check reserved bytes (must all be 0)
    if (header[1].toInt() != 0 || header[2].toInt() != 0 || header[3].toInt() != 0) {
        return false
    }

    // Parse and validate payload size
    val payloadSize = ((header[4].toInt() and 0xFF) shl 24) or
        ((header[5].toInt() and 0xFF) shl 16) or
        ((header[6].toInt() and 0xFF) shl 8) or
        (header[7].toInt() and 0xFF)

    // Payload size should be reasonable (not zero for a real frame, not too large)
    // Max reasonable size: 10MB per frame
    return payloadSize in 1..(10 * 1024 * 1024)
}

/**
 * Reads multiplexed stream from Docker (stdout/stderr format).
 *
 * Docker multiplexed stream format:
 * - Header: 8 bytes
 *   - Byte 0: Stream type (0=stdin, 1=stdout, 2=stderr)
 *   - Bytes 1-3: Reserved (0)
 *   - Bytes 4-7: Size of payload (big-endian uint32)
 * - Payload: N bytes of data
 */
private fun readMultiplexedStream(channel: ByteReadChannel): Flow<Frame> = flow {
    try {
        while (!channel.isClosedForRead) {
            val header = ByteArray(8)
            try {
                channel.readFully(header, 0, 8)
            } catch (_: Exception) {
                break
            }

            val streamTypeByte = header[0].toInt()
            val streamType = when (streamTypeByte) {
                0 -> Stream.StdIn
                1 -> Stream.StdOut
                2 -> Stream.StdErr
                else -> Stream.Unknown
            }

            // Parse payload size (big-endian uint32)
            val size = ((header[4].toInt() and 0xFF) shl 24) or
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

                emit(Frame(data.decodeToString(), size, streamType))
            }
        }
    } finally {
        channel.cancel()
    }
}

/**
 * Collects all multiplexed stream data into a single string.
 */
private suspend fun collectMultiplexedStream(channel: ByteReadChannel): String {
    val output = StringBuilder()
    readMultiplexedStream(channel).collect { frame ->
        output.append(frame.value)
    }
    return output.toString()
}


/**
 * Reads the stream with automatic TTY detection.
 *
 * Peeks at the first 8 bytes to determine if the stream uses Docker's
 * multiplexed protocol or is raw TTY output, then processes accordingly.
 */
private fun readStream(channel: ByteReadChannel): Flow<Frame> = flow {
    // Read first 8 bytes to detect stream type
    val header = ByteArray(8)
    var headerBytesRead = 0

    try {
        while (headerBytesRead < 8 && !channel.isClosedForRead) {
            val read = channel.readAvailable(header, headerBytesRead, 8 - headerBytesRead)
            if (read == -1) break
            headerBytesRead += read
        }
    } catch (e: Exception) {
        // If we can't even read the header, emit nothing
        return@flow
    }

    // Not enough data to determine
    if (headerBytesRead == 0) {
        return@flow
    }

    // Detect if multiplexed or raw TTY
    val isMultiplexed = headerBytesRead >= 8 && isMultiplexedStream(header)

    if (isMultiplexed) {
        // Process as multiplexed stream
        // First, process the header we already read
        val firstFrame = parseFrameFromHeader(header, channel)
        if (firstFrame != null) {
            emit(firstFrame)
        }

        // Continue reading remaining frames
        readMultiplexedFrames(channel).collect { emit(it) }
    } else {
        // Process as raw TTY stream
        // First, emit the header bytes as content (they're actual data, not protocol)
        val initialContent = header.copyOf(headerBytesRead).decodeToString()
        emit(Frame(initialContent, headerBytesRead, Stream.StdOut))

        // Continue reading raw data
        readRawFrames(channel).collect { emit(it) }
    }
}

/**
 * Parses a single frame from an already-read header.
 */
private suspend fun parseFrameFromHeader(header: ByteArray, channel: ByteReadChannel): Frame? {
    val streamType = Stream.typeOfOrNull(header[0]) ?: Stream.StdOut
    val size = ((header[4].toInt() and 0xFF) shl 24) or
        ((header[5].toInt() and 0xFF) shl 16) or
        ((header[6].toInt() and 0xFF) shl 8) or
        (header[7].toInt() and 0xFF)

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
private fun readMultiplexedFrames(channel: ByteReadChannel): Flow<Frame> = flow {
    try {
        while (!channel.isClosedForRead) {
            val header = ByteArray(8)
            try {
                channel.readFully(header, 0, 8)
            } catch (_: Exception) {
                break
            }

            val size = ((header[4].toInt() and 0xFF) shl 24) or
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
                emit(Frame(
                    value = data.decodeToString(),
                    length = size,
                    stream = Stream.typeOfOrNull(header[0]) ?: Stream.StdOut
                ))
            }
        }
    } finally {
        channel.cancel()
    }
}


/**
 * Continues reading raw TTY frames after the initial detection.
 */
private fun readRawFrames(channel: ByteReadChannel): Flow<Frame> = flow {
    try {
        val buffer = ByteArray(DefaultBufferSize)
        while (!channel.isClosedForRead) {
            val bytesRead = try {
                channel.readAvailable(buffer, 0, buffer.size)
            } catch (_: Exception) {
                break
            }

            if (bytesRead == -1) break

            if (bytesRead > 0) {
                emit(Frame(
                    value = buffer.copyOf(bytesRead).decodeToString(),
                    length = bytesRead,
                    stream = Stream.StdOut,
                ))
            }
        }
    } finally {
        channel.cancel()
    }
}

/**
 * Reads stream with auto-detection and separates stdout/stderr.
 * Only works for multiplexed streams; TTY streams return all as stdout.
 */
internal fun readStreamDemuxed(
    channel: ByteReadChannel
): Pair<Flow<String>, Flow<String>> {
    val frames = readStream(channel)

    val stdoutFlow = flow {
        frames.collect { frame ->
            if (frame.stream == Stream.StdOut) {
                emit(frame.value)
            }
        }
    }

    val stderrFlow = flow {
        frames.collect { frame ->
            if (frame.stream == Stream.StdErr) {
                emit(frame.value)
            }
        }
    }

    return stdoutFlow to stderrFlow
}

/**
 * Collects all frames into a single string.
 */
internal suspend fun collectStream(channel: ByteReadChannel): String {
    val output = StringBuilder()
    readStream(channel).collect { frame ->
        output.append(frame.value)
    }
    return output.toString()
}

/**
 * Collects frames with demuxing.
 */
internal suspend fun collectStreamDemuxed(
    channel: ByteReadChannel
): Pair<String, String> {
    val stdout = StringBuilder()
    val stderr = StringBuilder()

    readStream(channel).collect { frame ->
        when (frame.stream) {
            Stream.StdOut -> stdout.append(frame.value)
            Stream.StdOut -> stderr.append(frame.value)
            else -> {}
        }
    }

    return stdout.toString() to stderr.toString()
}