package me.devnatan.dockerkt.io

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlinx.io.readTo
import kotlin.math.min

public data class TarEntry(
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
    private const val BlockSize = 512
    private const val NameSize = 100
    private const val ModeSize = 8
    private const val UIDSize = 8
    private const val GIDSize = 8
    private const val SizeSize = 12
    private const val MTimeSize = 12
    private const val ChecksumSize = 8

    private const val TypeRegular = '0'.code.toByte()
    private const val TypeDirectory = '5'.code.toByte()

    fun createTarArchive(entries: List<TarEntry>): ByteArray {
        val buffer = Buffer()

        entries.forEach { entry ->
            writeEntry(buffer, entry)
        }

        // Write two empty blocks to mark end of archive
        buffer.write(ByteArray(BlockSize * 2))

        return buffer.readByteArray()
    }

    private fun writeEntry(
        buffer: Buffer,
        entry: TarEntry,
    ) {
        val header = ByteArray(BlockSize)

        writeName(header, entry.name) // Name (100 bytes)
        writeOctal(header, 100, entry.mode, ModeSize) // Mode (8 bytes) - octal
        writeOctal(header, 108, 0, UIDSize) // UID (8 bytes) - octal
        writeOctal(header, 116, 0, GIDSize) // GID (8 bytes) - octal
        writeOctal(header, 124, entry.size, SizeSize) // Size (12 bytes) - octal
        writeOctal(header, 136, entry.mtime, MTimeSize) // Mtime (12 bytes) - octal

        // Checksum (8 bytes) - initially fill with spaces
        repeat(ChecksumSize) { header[148 + it] = ' '.code.toByte() }

        // Type flag (1 byte)
        header[156] = if (entry.isDirectory) TypeDirectory else TypeRegular

        // Calculate and write checksum
        val checksum = header.sumOf { it.toInt() and 0xFF }
        writeOctal(header, 148, checksum.toLong(), ChecksumSize - 1)
        header[148 + ChecksumSize - 1] = 0 // Null terminator for checksum

        // Write header
        buffer.write(header)

        // Write data (if not a directory)
        if (!entry.isDirectory && entry.data != null) {
            buffer.write(entry.data)

            // Pad to block size
            val padding = BlockSize - (entry.data.size % BlockSize)
            if (padding < BlockSize) {
                buffer.write(ByteArray(padding))
            }
        }
    }

    fun extractTarArchive(tarData: ByteArray): List<TarEntry> {
        val buffer = Buffer()
        buffer.write(tarData)

        val entries = mutableListOf<TarEntry>()

        while (buffer.size >= BlockSize) {
            val header = ByteArray(BlockSize)
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
        val name = readString(header, 0, NameSize)
        val size = readOctal(header, 124, SizeSize)

        val mode = readOctal(header, 100, ModeSize)
        val mtime = readOctal(header, 136, MTimeSize)
        val typeFlag = header[156]

        val isDirectory = typeFlag == TypeDirectory

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
                val padding = BlockSize - (size % BlockSize).toInt()
                if (padding < BlockSize) {
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

    private fun writeName(
        dest: ByteArray,
        value: String,
    ) {
        val bytes = value.encodeToByteArray()
        val length = min(bytes.size, NameSize - 1) // Leave room for null terminator
        bytes.copyInto(
            destination = dest,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = length,
        )
        dest[length] = 0 // Null terminator
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

public object TarOperations {
    /**
     * Creates a tar archive from a single file.
     */
    public fun createTarFromFile(filePath: Path): ByteArray {
        val data = FileSystemUtils.readFile(filePath)
        val fileName = filePath.name

        val entry =
            TarEntry(
                name = fileName,
                size = data.size.toLong(),
                mode = 644, // Default file permissions
                mtime = FileSystemUtils.currentTimeSeconds(),
                isDirectory = false,
                data = data,
            )

        return TarUtils.createTarArchive(listOf(entry))
    }

    /**
     * Creates a tar archive from a directory recursively.
     */
    public fun createTarFromDirectory(
        dirPath: Path,
        basePath: String = "",
    ): ByteArray {
        val entries = mutableListOf<TarEntry>()
        collectEntriesFromDirectory(dirPath, basePath, entries)
        return TarUtils.createTarArchive(entries)
    }

    public fun collectDirectoryContents(
        dirPath: Path,
        basePath: String,
        entries: MutableList<TarEntry>,
    ) {
        val files = FileSystemUtils.listDirectory(dirPath)

        files.forEach { file ->
            val fileName = file.name
            val entryName = if (basePath.isEmpty()) fileName else "$basePath/$fileName"

            if (FileSystemUtils.isDirectory(file)) {
                // Add directory entry
                entries.add(
                    TarEntry(
                        name = "$entryName/",
                        size = 0,
                        mode = 755,
                        mtime = FileSystemUtils.currentTimeSeconds(),
                        isDirectory = true,
                        data = null,
                    ),
                )

                // Recursively add contents
                collectDirectoryContents(file, entryName, entries)
            } else {
                // Add file entry
                val data = FileSystemUtils.readFile(file)
                entries.add(
                    TarEntry(
                        name = entryName,
                        size = data.size.toLong(),
                        mode = 644,
                        mtime = FileSystemUtils.currentTimeSeconds(),
                        isDirectory = false,
                        data = data,
                    ),
                )
            }
        }
    }

    private fun collectEntriesFromDirectory(
        dirPath: Path,
        basePath: String,
        entries: MutableList<TarEntry>,
    ) {
        val files = FileSystemUtils.listDirectory(dirPath)

        files.forEach { file ->
            val fileName = file.name
            val entryName = if (basePath.isEmpty()) fileName else "$basePath/$fileName"

            if (FileSystemUtils.isDirectory(file)) {
                // Add directory entry
                entries.add(
                    TarEntry(
                        name = "$entryName/",
                        size = 0,
                        mode = 755, // Default directory permissions
                        mtime = FileSystemUtils.currentTimeSeconds(),
                        isDirectory = true,
                        data = null,
                    ),
                )

                // Recursively add contents
                collectEntriesFromDirectory(file, entryName, entries)
            } else {
                // Add file entry
                val data = FileSystemUtils.readFile(file)
                entries.add(
                    TarEntry(
                        name = entryName,
                        size = data.size.toLong(),
                        mode = 644, // Default file permissions
                        mtime = FileSystemUtils.currentTimeSeconds(),
                        isDirectory = false,
                        data = data,
                    ),
                )
            }
        }
    }

    /**
     * Extracts a tar archive to the local filesystem.
     */
    public fun extractTar(
        tarData: ByteArray,
        destinationPath: Path,
    ) {
        val entries = TarUtils.extractTarArchive(tarData)

        entries.forEach { entry ->
            val entryPath = Path(destinationPath, entry.name)

            if (entry.isDirectory) {
                FileSystemUtils.createDirectories(entryPath)
            } else {
                // Create parent directories
                entryPath.parent?.let { FileSystemUtils.createDirectories(it) }

                // Write file
                entry.data?.let { FileSystemUtils.writeFile(entryPath, it) }
            }
        }
    }
}
