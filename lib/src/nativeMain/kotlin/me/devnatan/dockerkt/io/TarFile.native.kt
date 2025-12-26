package me.devnatan.dockerkt.io

import kotlinx.io.RawSource

internal actual fun readTarFile(input: RawSource): RawSource = throw NotImplementedError()

internal actual fun writeTarFile(filePath: String): RawSource = throw NotImplementedError()
