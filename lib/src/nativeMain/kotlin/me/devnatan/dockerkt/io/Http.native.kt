package me.devnatan.dockerkt.io

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import me.devnatan.dockerkt.DockerClient

internal actual fun <T : HttpClientEngineConfig> HttpClientConfig<out T>.configureHttpClient(client: DockerClient) {
    TODO("Native HTTP client is not supported for now")
}
