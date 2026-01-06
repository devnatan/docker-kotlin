package me.devnatan.dockerkt.io

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import me.devnatan.dockerkt.DockerClient

internal actual fun createHttpClient(dockerClient: DockerClient): HttpClient {
    TODO("Native HTTP client is not supported for now")
}
