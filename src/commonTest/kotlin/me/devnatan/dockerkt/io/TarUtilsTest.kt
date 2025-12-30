package me.devnatan.dockerkt.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TarUtilsTest {
    @Test
    fun `create and extract single file tar archive`() {
        val fileName = "test.txt"
        val content = "Hello, World!".encodeToByteArray()

        val entry =
            TarEntry(
                name = fileName,
                size = content.size.toLong(),
                isDirectory = false,
                data = content,
                mode = 644,
                mtime = 1234567890,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        val extractedEntry = extracted[0]

        assertEquals(fileName, extractedEntry.name)
        assertEquals(content.size.toLong(), extractedEntry.size)
        assertEquals(644L, extractedEntry.mode)
        assertEquals(1234567890L, extractedEntry.mtime)
        assertFalse(extractedEntry.isDirectory)
        assertNotNull(extractedEntry.data)
        assertContentEquals(content, extractedEntry.data)
    }

    @Test
    fun `create and extract multiple files tar archive`() {
        val entries =
            listOf(
                TarEntry(
                    name = "file1.txt",
                    size = 5,
                    mode = 644,
                    mtime = 1000000,
                    isDirectory = false,
                    data = "file1".encodeToByteArray(),
                ),
                TarEntry(
                    name = "file2.txt",
                    size = 5,
                    mode = 644,
                    mtime = 1000001,
                    isDirectory = false,
                    data = "file2".encodeToByteArray(),
                ),
                TarEntry(
                    name = "file3.txt",
                    size = 5,
                    mode = 755,
                    mtime = 1000002,
                    isDirectory = false,
                    data = "file3".encodeToByteArray(),
                ),
            )

        val tarData = TarUtils.createTarArchive(entries)
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(3, extracted.size)

        assertEquals("file1.txt", extracted[0].name)
        assertEquals("file1", extracted[0].data?.decodeToString())

        assertEquals("file2.txt", extracted[1].name)
        assertEquals("file2", extracted[1].data?.decodeToString())

        assertEquals("file3.txt", extracted[2].name)
        assertEquals("file3", extracted[2].data?.decodeToString())
        assertEquals(755L, extracted[2].mode)
    }

    @Test
    fun `create and extract directory entry`() {
        val entry =
            TarEntry(
                name = "testdir/",
                size = 0,
                mode = 755,
                mtime = 1234567890,
                isDirectory = true,
                data = null,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        val extractedEntry = extracted[0]

        assertEquals("testdir/", extractedEntry.name)
        assertEquals(0L, extractedEntry.size)
        assertEquals(755L, extractedEntry.mode)
        assertTrue(extractedEntry.isDirectory)
        assertNotNull(extractedEntry.data)
        assertTrue(extractedEntry.data.isEmpty())
    }

    @Test
    fun `create and extract nested directory structure`() {
        val entries =
            listOf(
                TarEntry(
                    name = "parent/",
                    size = 0,
                    mode = 755,
                    mtime = 1000000,
                    isDirectory = true,
                    data = null,
                ),
                TarEntry(
                    name = "parent/child/",
                    size = 0,
                    mode = 755,
                    mtime = 1000001,
                    isDirectory = true,
                    data = null,
                ),
                TarEntry(
                    name = "parent/child/file.txt",
                    size = 7,
                    mode = 644,
                    mtime = 1000002,
                    isDirectory = false,
                    data = "content".encodeToByteArray(),
                ),
            )

        val tarData = TarUtils.createTarArchive(entries)
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(3, extracted.size)

        assertTrue(extracted[0].isDirectory)
        assertEquals("parent/", extracted[0].name)

        assertTrue(extracted[1].isDirectory)
        assertEquals("parent/child/", extracted[1].name)

        assertFalse(extracted[2].isDirectory)
        assertEquals("parent/child/file.txt", extracted[2].name)
        assertEquals("content", extracted[2].data?.decodeToString())
    }

    @Test
    fun `create and extract empty file`() {
        val entry =
            TarEntry(
                name = "empty.txt",
                size = 0,
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = ByteArray(0),
            )

        val tarData = TarUtils.createTarArchive(entries = listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        assertEquals("empty.txt", extracted[0].name)
        assertEquals(0L, extracted[0].size)
        assertNotNull(extracted[0].data)
        assertEquals(0, extracted[0].data!!.size)
    }

    @Test
    fun `create and extract large file`() {
        val largeContent = "X".repeat(10000).encodeToByteArray()

        val entry =
            TarEntry(
                name = "large.txt",
                size = largeContent.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = largeContent,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        assertEquals("large.txt", extracted[0].name)
        assertEquals(largeContent.size.toLong(), extracted[0].size)
        assertContentEquals(largeContent, extracted[0].data)
    }

    @Test
    fun `create and extract file with special characters in name`() {
        val entries =
            listOf(
                TarEntry(
                    name = "file-with-dash.txt",
                    size = 4,
                    mode = 644,
                    mtime = 1000000,
                    isDirectory = false,
                    data = "test".encodeToByteArray(),
                ),
                TarEntry(
                    name = "file_with_underscore.txt",
                    size = 4,
                    mode = 644,
                    mtime = 1000001,
                    isDirectory = false,
                    data = "test".encodeToByteArray(),
                ),
                TarEntry(
                    name = "file.with.dots.txt",
                    size = 4,
                    mode = 644,
                    mtime = 1000002,
                    isDirectory = false,
                    data = "test".encodeToByteArray(),
                ),
            )

        val tarData = TarUtils.createTarArchive(entries)
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(3, extracted.size)
        assertEquals("file-with-dash.txt", extracted[0].name)
        assertEquals("file_with_underscore.txt", extracted[1].name)
        assertEquals("file.with.dots.txt", extracted[2].name)
    }

    @Test
    fun `create and extract file with long name`() {
        // TAR standard supports names up to 100 characters
        val longName = "a".repeat(99) + ".txt"
        val content = "test content".encodeToByteArray()

        val entry =
            TarEntry(
                name = longName,
                size = content.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = content,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        // Name should be truncated to fit in TAR header
        assertTrue(extracted[0].name.length <= 100)
        assertTrue(extracted[0].name.startsWith("aaa"))
    }

    @Test
    fun `create and extract binary file`() {
        val binaryContent =
            byteArrayOf(
                0x00,
                0x01,
                0x02,
                0x03,
                0xFF.toByte(),
                0xFE.toByte(),
                0xFD.toByte(),
                0xFC.toByte(),
                0x89.toByte(),
                0x50,
                0x4E,
                0x47, // PNG header
            )

        val entry =
            TarEntry(
                name = "binary.bin",
                size = binaryContent.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = binaryContent,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        assertContentEquals(binaryContent, extracted[0].data)
    }

    @Test
    fun `create and extract file with different permissions`() {
        val entries =
            listOf(
                TarEntry(
                    name = "readonly.txt",
                    size = 4,
                    mode = 444, // Read-only
                    mtime = 1000000,
                    isDirectory = false,
                    data = "test".encodeToByteArray(),
                ),
                TarEntry(
                    name = "writable.txt",
                    size = 4,
                    mode = 644, // Read-write
                    mtime = 1000001,
                    isDirectory = false,
                    data = "test".encodeToByteArray(),
                ),
                TarEntry(
                    name = "executable.sh",
                    size = 4,
                    mode = 755, // Executable
                    mtime = 1000002,
                    isDirectory = false,
                    data = "test".encodeToByteArray(),
                ),
            )

        val tarData = TarUtils.createTarArchive(entries)
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(3, extracted.size)
        assertEquals(444L, extracted[0].mode)
        assertEquals(644L, extracted[1].mode)
        assertEquals(755L, extracted[2].mode)
    }

    @Test
    fun `create and extract empty archive`() {
        val tarData = TarUtils.createTarArchive(emptyList())
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(0, extracted.size)
    }

    @Test
    fun `create and extract mixed files and directories`() {
        val entries =
            listOf(
                TarEntry(
                    name = "dir1/",
                    size = 0,
                    mode = 755,
                    mtime = 1000000,
                    isDirectory = true,
                    data = null,
                ),
                TarEntry(
                    name = "dir1/file1.txt",
                    size = 6,
                    mode = 644,
                    mtime = 1000001,
                    isDirectory = false,
                    data = "file1\n".encodeToByteArray(),
                ),
                TarEntry(
                    name = "dir2/",
                    size = 0,
                    mode = 755,
                    mtime = 1000002,
                    isDirectory = true,
                    data = null,
                ),
                TarEntry(
                    name = "dir2/subdir/",
                    size = 0,
                    mode = 755,
                    mtime = 1000003,
                    isDirectory = true,
                    data = null,
                ),
                TarEntry(
                    name = "dir2/subdir/file2.txt",
                    size = 6,
                    mode = 644,
                    mtime = 1000004,
                    isDirectory = false,
                    data = "file2\n".encodeToByteArray(),
                ),
                TarEntry(
                    name = "root.txt",
                    size = 5,
                    mode = 644,
                    mtime = 1000005,
                    isDirectory = false,
                    data = "root\n".encodeToByteArray(),
                ),
            )

        val tarData = TarUtils.createTarArchive(entries)
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(6, extracted.size)

        // Verify structure
        assertTrue(extracted[0].isDirectory)
        assertEquals("dir1/", extracted[0].name)

        assertFalse(extracted[1].isDirectory)
        assertEquals("dir1/file1.txt", extracted[1].name)
        assertEquals("file1\n", extracted[1].data?.decodeToString())

        assertTrue(extracted[2].isDirectory)
        assertEquals("dir2/", extracted[2].name)

        assertTrue(extracted[3].isDirectory)
        assertEquals("dir2/subdir/", extracted[3].name)

        assertFalse(extracted[4].isDirectory)
        assertEquals("dir2/subdir/file2.txt", extracted[4].name)
        assertEquals("file2\n", extracted[4].data?.decodeToString())

        assertFalse(extracted[5].isDirectory)
        assertEquals("root.txt", extracted[5].name)
        assertEquals("root\n", extracted[5].data?.decodeToString())
    }

    @Test
    fun `tar archive has correct structure`() {
        val content = "test".encodeToByteArray()
        val entry =
            TarEntry(
                name = "test.txt",
                size = content.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = content,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))

        // TAR format:
        // - 512 bytes header
        // - Data (padded to 512 byte blocks)
        // - 1024 bytes (2 empty blocks) at end

        val expectedSize =
            512 + // Header
                512 + // Data block (4 bytes + padding)
                1024 // End markers

        assertEquals(expectedSize, tarData.size)
    }

    @Test
    fun `extract handles padding correctly`() {
        // File size not multiple of 512 should be padded
        val content = "A".repeat(100).encodeToByteArray() // 100 bytes

        val entry =
            TarEntry(
                name = "padded.txt",
                size = content.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = content,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        assertEquals(100L, extracted[0].size)
        assertContentEquals(content, extracted[0].data)
    }

    @Test
    fun `roundtrip preserves all metadata`() {
        val originalEntry =
            TarEntry(
                name = "metadata-test.txt",
                size = 13,
                mode = 755,
                mtime = 1234567890,
                isDirectory = false,
                data = "test content!".encodeToByteArray(),
            )

        val tarData = TarUtils.createTarArchive(listOf(originalEntry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        val roundtripEntry = extracted[0]

        assertEquals(originalEntry.name, roundtripEntry.name)
        assertEquals(originalEntry.size, roundtripEntry.size)
        assertEquals(originalEntry.mode, roundtripEntry.mode)
        assertEquals(originalEntry.mtime, roundtripEntry.mtime)
        assertEquals(originalEntry.isDirectory, roundtripEntry.isDirectory)
        assertContentEquals(originalEntry.data, roundtripEntry.data)
    }

    @Test
    fun `file with exact block size boundary`() {
        // 512 bytes exactly (one block)
        val content = "X".repeat(512).encodeToByteArray()

        val entry =
            TarEntry(
                name = "block-size.txt",
                size = content.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = content,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        assertEquals(512L, extracted[0].size)
        assertContentEquals(content, extracted[0].data)
    }

    @Test
    fun `file with size just over block boundary`() {
        // 513 bytes (needs 2 blocks)
        val content = "X".repeat(513).encodeToByteArray()

        val entry =
            TarEntry(
                name = "over-block.txt",
                size = content.size.toLong(),
                mode = 644,
                mtime = 1234567890,
                isDirectory = false,
                data = content,
            )

        val tarData = TarUtils.createTarArchive(listOf(entry))
        val extracted = TarUtils.extractTarArchive(tarData)

        assertEquals(1, extracted.size)
        assertEquals(513L, extracted[0].size)
        assertContentEquals(content, extracted[0].data)
    }
}
