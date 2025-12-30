package me.devnatan.dockerkt.io

import kotlinx.io.buffered
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Multiplatform file operations wrapper. */
internal object FileSystemUtils {
    private val fs = SystemFileSystem

    fun readFile(path: Path): ByteArray =
        fs.source(path).buffered().use { source ->
            source.readByteArray()
        }

    fun writeFile(
        path: Path,
        data: ByteArray,
    ) = fs.sink(path).buffered().use { sink ->
        sink.write(data)
    }

    fun createDirectories(path: Path) {
        fs.createDirectories(path)
    }

    fun exists(path: Path): Boolean = fs.exists(path)

    fun isDirectory(path: Path): Boolean = fs.metadataOrNull(path)?.isDirectory ?: false

    fun listDirectory(path: Path): List<Path> = fs.list(path).toList()

    fun getMetadata(path: Path): FileMetadata? = fs.metadataOrNull(path)

    @OptIn(ExperimentalTime::class)
    fun currentTimeSeconds(): Long = Clock.System.now().epochSeconds
}
