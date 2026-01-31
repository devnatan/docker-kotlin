package me.devnatan.dockerkt.io

import io.ktor.utils.io.core.toByteArray
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiplexedStreamParsingTest {
    @Test
    fun `parse stdout frame`() {
        val content = "Hello, World!"
        val header = createMultiplexHeader(streamType = 1, size = content.length)
        val data = header + content.encodeToByteArray()

        val frame = parseMultiplexedFrame(data)

        assertEquals(Stream.StdOut, frame.stream)
        assertEquals("Hello, World!", frame.value)
    }

    @Test
    fun `parse stderr frame`() {
        val content = "Error message"
        val header = createMultiplexHeader(streamType = 2, size = content.length)
        val data = header + content.encodeToByteArray()

        val frame = parseMultiplexedFrame(data)

        assertEquals(Stream.StdErr, frame.stream)
        assertEquals("Error message", frame.value)
    }

    @Test
    fun `parse stdin frame`() {
        val content = "input"
        val header = createMultiplexHeader(streamType = 0, size = content.length)
        val data = header + content.encodeToByteArray()

        val frame = parseMultiplexedFrame(data)

        assertEquals(Stream.StdIn, frame.stream)
        assertEquals("input", frame.value)
    }

    @Test
    fun `parse empty frame`() {
        val header = createMultiplexHeader(streamType = 1, size = 0)

        val frame = parseMultiplexedFrame(header)

        assertEquals(Stream.StdOut, frame.stream)
        assertEquals("", frame.value)
    }

    @Test
    fun `parse large frame`() {
        val content = "A".repeat(65536) // 64KB
        val header = createMultiplexHeader(streamType = 1, size = content.length)
        val data = header + content.encodeToByteArray()

        val frame = parseMultiplexedFrame(data)

        assertEquals(Stream.StdOut, frame.stream)
        assertEquals(65536, frame.value.length)
    }

    @Test
    fun `parse frame with unicode content`() {
        val content = "Hello 世界 🌍"
        val contentBytes = content.encodeToByteArray()
        val header = createMultiplexHeader(streamType = 1, size = contentBytes.size)
        val data = header + contentBytes

        val frame = parseMultiplexedFrame(data)

        assertEquals(Stream.StdOut, frame.stream)
        assertEquals("Hello 世界 🌍", frame.value)
    }

    @Test
    fun `parse frame with newlines`() {
        val content = "Line 1\nLine 2\nLine 3\n"
        val header = createMultiplexHeader(streamType = 1, size = content.length)
        val data = header + content.encodeToByteArray()

        val frame = parseMultiplexedFrame(data)

        assertEquals(Stream.StdOut, frame.stream)
        assertEquals("Line 1\nLine 2\nLine 3\n", frame.value)
    }

    // Helper functions for testing
    // See https://docs.docker.com/reference/api/engine/version/v1.52/#tag/Container/operation/ContainerAttach
    private fun createMultiplexHeader(
        streamType: Int,
        size: Int,
    ): ByteArray =
        byteArrayOf(
            streamType.toByte(),
            0,
            0,
            0, // Reserved bytes
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte(),
        )

    private fun parseMultiplexedFrame(data: ByteArray): Frame {
        val streamTypeByte = data[0].toInt()
        val streamType =
            when (streamTypeByte) {
                0 -> Stream.StdIn
                1 -> Stream.StdOut
                2 -> Stream.StdErr
                else -> Stream.StdOut
            }

        val size =
            ((data[4].toInt() and 0xFF) shl 24) or
                ((data[5].toInt() and 0xFF) shl 16) or
                ((data[6].toInt() and 0xFF) shl 8) or
                (data[7].toInt() and 0xFF)

        val content =
            if (size > 0 && data.size > 8) {
                data.copyOfRange(8, 8 + size).decodeToString()
            } else {
                ""
            }

        return Frame(content, size, streamType)
    }

    @Test
    fun `isMultiplexedStream should detect valid stdout header`() {
        // Stream type = STDOUT (1), size = 5
        val header =
            byteArrayOf(
                1,
                0,
                0,
                0, // Type (stdout) + reserved
                0,
                0,
                0,
                5, // Size = 5
            )

        assertTrue(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should detect valid stderr header`() {
        // Stream type = STDERR (2), size = 10
        val header =
            byteArrayOf(
                2,
                0,
                0,
                0, // Type (stderr) + reserved
                0,
                0,
                0,
                10, // Size = 10
            )

        assertTrue(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should detect valid stdin header`() {
        // Stream type = STDIN (0), size = 3
        val header =
            byteArrayOf(
                0,
                0,
                0,
                0, // Type (stdin) + reserved
                0,
                0,
                0,
                3, // Size = 3
            )

        assertTrue(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should reject invalid stream type`() {
        // Invalid stream type (5)
        val header =
            byteArrayOf(
                5,
                0,
                0,
                0,
                0,
                0,
                0,
                10,
            )

        assertFalse(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should reject non-zero reserved bytes`() {
        val header =
            byteArrayOf(
                1,
                1,
                0,
                0, // Reserved byte 1 is not 0
                0,
                0,
                0,
                10,
            )

        assertFalse(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should reject zero payload size`() {
        val header =
            byteArrayOf(
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0, // Size = 0
            )

        assertFalse(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should reject header too short`() {
        val header = byteArrayOf(1, 0, 0, 0)
        assertFalse(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should detect raw TTY text data`() {
        // Raw text "Hello" - not a valid multiplexed header
        val header = "Hello Wo".toByteArray()
        assertFalse(isMultiplexedStream(header))
    }

    @Test
    fun `isMultiplexedStream should detect ANSI escape sequences as raw`() {
        // ANSI escape sequence for colors
        val header = "\u001B[32mTest".toByteArray()
        assertFalse(isMultiplexedStream(header))
    }

    @Test
    fun `readPayloadSize should parse small size`() {
        val header = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 5)
        assertEquals(5, readPayloadSize(header))
    }

    @Test
    fun `readPayloadSize should parse medium size`() {
        // Size = 256
        val header = byteArrayOf(1, 0, 0, 0, 0, 0, 1, 0)
        assertEquals(256, readPayloadSize(header))
    }

    @Test
    fun `readPayloadSize should parse large size`() {
        // Size = 65536 (0x00010000)
        val header = byteArrayOf(1, 0, 0, 0, 0, 1, 0, 0)
        assertEquals(65536, readPayloadSize(header))
    }

    @Test
    fun `readPayloadSize should parse maximum reasonable size`() {
        // Size = 10MB (10 * 1024 * 1024 = 10485760 = 0x00A00000)
        val header =
            byteArrayOf(
                1,
                0,
                0,
                0,
                0,
                0xA0.toByte(),
                0,
                0,
            )
        assertEquals(10485760, readPayloadSize(header))
    }
}
