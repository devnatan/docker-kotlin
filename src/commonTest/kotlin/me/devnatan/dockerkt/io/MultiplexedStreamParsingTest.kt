package me.devnatan.dockerkt.io

import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.Stream
import kotlin.test.Test
import kotlin.test.assertEquals

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
    private fun createMultiplexHeader(streamType: Int, size: Int): ByteArray {
        return byteArrayOf(
            streamType.toByte(),
            0, 0, 0, // Reserved bytes
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte()
        )
    }

    private fun parseMultiplexedFrame(data: ByteArray): Frame {
        val streamTypeByte = data[0].toInt()
        val streamType = when (streamTypeByte) {
            0 -> Stream.StdIn
            1 -> Stream.StdOut
            2 -> Stream.StdErr
            else -> Stream.StdOut
        }

        val size = ((data[4].toInt() and 0xFF) shl 24) or
            ((data[5].toInt() and 0xFF) shl 16) or
            ((data[6].toInt() and 0xFF) shl 8) or
            (data[7].toInt() and 0xFF)

        val content = if (size > 0 && data.size > 8) {
            data.copyOfRange(8, 8 + size).decodeToString()
        } else {
            ""
        }

        return Frame(content, size, streamType)
    }
}