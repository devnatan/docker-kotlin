package me.devnatan.dockerkt.io

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import me.devnatan.dockerkt.DockerClient

internal actual val defaultHttpClientEngine: HttpClientEngineFactory<*>? = CIO

internal actual fun <T : HttpClientEngineConfig> HttpClientConfig<out T>.configureHttpClient(client: DockerClient) {
    defaultRequest {
        unixSocket(client.config.socketPath)
    }
}
