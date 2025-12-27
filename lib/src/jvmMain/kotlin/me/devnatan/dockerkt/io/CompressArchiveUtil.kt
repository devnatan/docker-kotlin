package me.devnatan.dockerkt.io

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name
import kotlin.io.path.relativeTo

internal object CompressArchiveUtil {
    @Throws(IOException::class)
    fun tar(
        inputPath: Path,
        outputPath: Path,
        childrenOnly: Boolean,
    ) {
        buildTarStream(outputPath).use { tarArchiveOutputStream ->
            if (!Files.isDirectory(inputPath)) {
                addFileToTar(tarArchiveOutputStream, inputPath, inputPath.fileName.toString())
            } else {
                var sourcePath: Path = inputPath
                if (!childrenOnly) {
                    // In order to have the dossier as the root entry
                    sourcePath = inputPath.parent
                }
                Files.walkFileTree(inputPath, TarDirWalker(sourcePath, tarArchiveOutputStream))
            }
            tarArchiveOutputStream.flush()
        }
    }

    @Throws(IOException::class)
    fun addFileToTar(
        tarArchiveOutputStream: TarArchiveOutputStream,
        file: Path,
        entryName: String?,
    ) {
        if (Files.isSymbolicLink(file)) {
            tarArchiveOutputStream.putArchiveEntry(
                TarArchiveEntry(entryName, TarArchiveEntry.LF_SYMLINK).apply {
                    linkName = Files.readSymbolicLink(file).toString()
                },
            )
        } else {
            val tarArchiveEntry =
                tarArchiveOutputStream.createArchiveEntry(
                    file.toFile(),
                    entryName,
                ) as TarArchiveEntry
            if (file.toFile().canExecute()) {
                tarArchiveEntry.mode = tarArchiveEntry.mode or 493
            }
            tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry)
            if (file.toFile().isFile()) {
                BufferedInputStream(Files.newInputStream(file)).use { input ->
                    input.copyTo(tarArchiveOutputStream)
                }
            }
        }
        tarArchiveOutputStream.closeArchiveEntry()
    }

    @Throws(IOException::class)
    private fun buildTarStream(outputPath: Path) =
        TarArchiveOutputStream(GzipCompressorOutputStream(BufferedOutputStream(Files.newOutputStream(outputPath))))
            .apply {
                setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            }

    class TarDirWalker(
        private val basePath: Path,
        private val tarArchiveOutputStream: TarArchiveOutputStream,
    ) : SimpleFileVisitor<Path>() {
        @Throws(IOException::class)
        override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes,
        ): FileVisitResult {
            if (dir != basePath) {
                tarArchiveOutputStream.putArchiveEntry(
                    TarArchiveEntry(dir.toFile(), dir.relativeTo(basePath).fileName.name),
                )
                tarArchiveOutputStream.closeArchiveEntry()
            }
            return FileVisitResult.CONTINUE
        }

        @Throws(IOException::class)
        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
        ): FileVisitResult {
            addFileToTar(tarArchiveOutputStream, file, file.relativeTo(basePath).toString())
            return FileVisitResult.CONTINUE
        }

        @Throws(IOException::class)
        override fun visitFileFailed(
            file: Path,
            exc: IOException,
        ): FileVisitResult {
            tarArchiveOutputStream.close()
            throw exc
        }
    }
}
