package me.devnatan.dockerkt.io

import kotlinx.io.RawSource
import kotlinx.io.asInputStream
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.inputStream

internal actual fun readTarFile(input: RawSource): RawSource =
    TarArchiveInputStream(input.buffered().asInputStream())
        .apply {
            nextTarEntry
        }.asSource()

internal actual fun writeTarFile(filePath: String): RawSource {
    val output = Files.createTempFile("dockerkt", ".tar.gz")
    CompressArchiveUtil.tar(
        inputPath = Paths.get(filePath),
        outputPath = output,
        childrenOnly = false,
    )
    return output.inputStream().asSource()
}
