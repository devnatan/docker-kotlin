package me.devnatan.dockerkt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

/**
 * Returns if the current platform is a UNIX-based platform.
 */
internal actual fun isUnixPlatform(): Boolean = true

/**
 * Gets the value of the specified environment variable.
 * An environment variable is a system-dependent external named value
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun env(key: String): String? = getenv(key)?.toKString()

public actual interface Closeable {
    public actual fun close()
}
