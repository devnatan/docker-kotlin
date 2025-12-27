package me.devnatan.dockerkt

internal actual fun env(key: String): String? = System.getenv(key)

internal actual fun isUnixPlatform(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    return arrayOf("nix", "nux", "anix", "mac").any { os.contains(it) }
}

public actual typealias Closeable = java.io.Closeable
