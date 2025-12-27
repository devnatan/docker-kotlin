package me.devnatan.dockerkt

/**
 * Gets the value of the specified environment variable.
 * An environment variable is a system-dependent external named value
 */
internal expect fun env(key: String): String?

/**
 * Returns if the current platform is a UNIX-based platform.
 */
internal expect fun isUnixPlatform(): Boolean

public expect interface Closeable {
    public fun close()
}
