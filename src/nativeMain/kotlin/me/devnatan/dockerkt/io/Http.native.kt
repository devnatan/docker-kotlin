package me.devnatan.dockerkt.io

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
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
            unixSocket(socketPath.removePrefix(UnixSocketPrefix))
        }
        // Force Connection: close. Docker often returns bodies framed by connection-close (no
        // Content-Length, no Transfer-Encoding), and Ktor CIO over unix sockets cannot otherwise
        // determine where the response ends — throwing "request body length should be specified,
        // chunked transfer encoding should be used or keep-alive should be disabled (connection: close)".
        header(HttpHeaders.Connection, "close")
    }
}
