package me.devnatan.dockerkt.io

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import me.devnatan.dockerkt.DockerClient

internal actual val defaultHttpClientEngine: HttpClientEngineFactory<*>? get() = CIO

internal actual fun <T : HttpClientEngineConfig> HttpClientConfig<out T>.configureHttpClient(client: DockerClient) {
    engine {
        require(this is io.ktor.client.engine.cio.CIOEngineConfig) { "Only CIO engine is supported for now" }
        // disable request timeout so long-running calls (image pulls, log streams) aren't killed
        requestTimeout = 0
    }
    defaultRequest {
        val socketPath = client.config.socketPath
        if (isUnixSocket(socketPath)) {
            unixSocket(socketPath)
        }
    }
}
