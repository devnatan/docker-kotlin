package me.devnatan.dockerkt.io

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import kotlinx.io.readTo
import kotlin.math.min

internal expect fun readTarFile(input: RawSource): RawSource

internal expect fun writeTarFile(filePath: String): RawSource

internal data class TarEntry(
    val name: String,
    val size: Long,
    val mode: Long,
    val mtime: Long,
    val isDirectory: Boolean,
    val data: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TarEntry

        if (name != other.name) return false
        if (size != other.size) return false
        if (mode != other.mode) return false
        if (mtime != other.mtime) return false
        if (isDirectory != other.isDirectory) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + mtime.hashCode()
        result = 31 * result + isDirectory.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}

internal object TarUtils {
    private const val BLOCK_SIZE = 512
    private const val NAME_SIZE = 100
    private const val MODE_SIZE = 8
    private const val UID_SIZE = 8
    private const val GID_SIZE = 8
    private const val SIZE_SIZE = 12
    private const val MTIME_SIZE = 12
    private const val CHECKSUM_SIZE = 8
    private const val TYPE_SIZE = 1

    private const val TYPE_REGULAR = '0'.code.toByte()
    private const val TYPE_DIRECTORY = '5'.code.toByte()

    fun createTarArchive(entries: List<TarEntry>): ByteArray {
        val buffer = Buffer()

        entries.forEach { entry ->
            writeEntry(buffer, entry)
        }

        // Write two empty blocks to mark end of archive
        buffer.write(ByteArray(BLOCK_SIZE * 2))

        return buffer.readByteArray()
    }

    private fun writeEntry(
        buffer: Buffer,
        entry: TarEntry,
    ) {
        val header = ByteArray(BLOCK_SIZE)

        writeString(header, 0, entry.name, NAME_SIZE) // Name (100 bytes)
        writeOctal(header, 100, entry.mode, MODE_SIZE) // Mode (8 bytes) - octal
        writeOctal(header, 108, 0, UID_SIZE) // UID (8 bytes) - octal
        writeOctal(header, 116, 0, GID_SIZE) // GID (8 bytes) - octal
        writeOctal(header, 124, entry.size, SIZE_SIZE) // Size (12 bytes) - octal
        writeOctal(header, 136, entry.mtime, MTIME_SIZE) // Mtime (12 bytes) - octal

        // Checksum (8 bytes) - initially fill with spaces
        repeat(CHECKSUM_SIZE) { header[148 + it] = ' '.code.toByte() }

        // Type flag (1 byte)
        header[156] = if (entry.isDirectory) TYPE_DIRECTORY else TYPE_REGULAR

        // Calculate and write checksum
        val checksum = header.sumOf { it.toInt() and 0xFF }
        writeOctal(header, 148, checksum.toLong(), CHECKSUM_SIZE - 1)
        header[148 + CHECKSUM_SIZE - 1] = 0 // Null terminator for checksum

        // Write header
        buffer.write(header)

        // Write data (if not a directory)
        if (!entry.isDirectory && entry.data != null) {
            buffer.write(entry.data)

            // Pad to block size
            val padding = BLOCK_SIZE - (entry.data.size % BLOCK_SIZE)
            if (padding < BLOCK_SIZE) {
                buffer.write(ByteArray(padding))
            }
        }
    }

    fun extractTarArchive(tarData: ByteArray): List<TarEntry> {
        val buffer = Buffer()
        buffer.write(tarData)

        val entries = mutableListOf<TarEntry>()

        while (buffer.size >= BLOCK_SIZE) {
            val header = ByteArray(BLOCK_SIZE)
            buffer.readTo(header)

            // Check if this is an empty block (end of archive)
            if (header.all { it == 0.toByte() }) {
                break
            }

            val entry = parseEntry(buffer, header)
            entries.add(entry)
        }

        return entries
    }

    /** Parses a TAR entry from header and buffer. */
    private fun parseEntry(
        buffer: Buffer,
        header: ByteArray,
    ): TarEntry {
        val name = readString(header, 0, NAME_SIZE)
        val size = readOctal(header, 124, SIZE_SIZE)

        val mode = readOctal(header, 100, MODE_SIZE)
        val mtime = readOctal(header, 136, MTIME_SIZE)
        val typeFlag = header[156]

        val isDirectory = typeFlag == TYPE_DIRECTORY

        if (size == 0L) {
            return TarEntry(
                name = name,
                size = size,
                mode = mode,
                mtime = mtime,
                isDirectory = isDirectory,
                data = byteArrayOf(),
            )
        }

        val data =
            if (!isDirectory && size > 0) {
                val fileData = ByteArray(size.toInt())
                buffer.readTo(fileData)

                // Skip padding
                val padding = BLOCK_SIZE - (size % BLOCK_SIZE).toInt()
                if (padding < BLOCK_SIZE) {
                    buffer.skip(padding.toLong())
                }

                fileData
            } else {
                null
            }

        return TarEntry(
            name = name,
            size = size,
            mode = mode,
            mtime = mtime,
            isDirectory = isDirectory,
            data = data,
        )
    }

    private fun writeString(
        dest: ByteArray,
        offset: Int,
        value: String,
        maxLength: Int,
    ) {
        val bytes = value.encodeToByteArray()
        val length = min(bytes.size, maxLength - 1) // Leave room for null terminator
        bytes.copyInto(dest, offset, 0, length)
        dest[offset + length] = 0 // Null terminator
    }

    /** Writes an octal number to a byte array at the given offset.*/
    private fun writeOctal(
        dest: ByteArray,
        offset: Int,
        value: Long,
        maxLength: Int,
    ) {
        val octal = value.toString(8)
        val length = min(octal.length, maxLength - 1)
        octal.takeLast(length).forEachIndexed { index, char ->
            dest[offset + index] = char.code.toByte()
        }
        dest[offset + length] = 0 // Null terminator
    }

    /** Reads a null-terminated string from a byte array. */
    private fun readString(
        source: ByteArray,
        offset: Int,
        maxLength: Int,
    ): String {
        val end = (offset until offset + maxLength).firstOrNull { source[it] == 0.toByte() } ?: (offset + maxLength)
        return source.decodeToString(offset, end)
    }

    private fun readOctal(
        source: ByteArray,
        offset: Int,
        maxLength: Int,
    ): Long {
        val str = readString(source, offset, maxLength).trim()
        return if (str.isEmpty()) 0 else str.toLongOrNull(8) ?: 0
    }
}
