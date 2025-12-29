package me.devnatan.dockerkt.io

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.okhttp.OkHttpConfig
import me.devnatan.dockerkt.DockerClient
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

// Ktor doesn't allow us to change "Upgrade" header so we set it directly in the engine
private class UpgradeHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.url.encodedPath.matches(Regex(".*/exec/.*/start$"))) {
            try {
                val newRequest =
                    request
                        .newBuilder()
                        .header("Connection", "Upgrade")
                        .header("Upgrade", "tcp")
                        .build()

                return chain.proceed(newRequest)
            } catch (e: IllegalArgumentException) {
                if (e.message.equals("expected a null or empty request body with 'Connection: upgrade'")) {
                    return chain.proceed(request)
                }
            }
        }

        return chain.proceed(request)
    }
}

internal actual fun <T : HttpClientEngineConfig> HttpClientConfig<out T>.configureHttpClient(client: DockerClient) {
    engine {
        // ensure that current engine is OkHttp, cannot use CIO due to a Ktor Client bug related to data streaming
        // https://youtrack.jetbrains.com/issue/KTOR-2494
        require(this is OkHttpConfig) { "Only OkHttp engine is supported for now" }

        config {
            val isUnixSocket = isUnixSocket(client.config.socketPath)
            if (isUnixSocket) {
                socketFactory(UnixSocketFactory())
            }
            dns(SocketDns(isUnixSocket))
            readTimeout(0, TimeUnit.MILLISECONDS)
            connectTimeout(0, TimeUnit.MILLISECONDS)
            callTimeout(0, TimeUnit.MILLISECONDS)
            retryOnConnectionFailure(true)
            addInterceptor(UpgradeHeaderInterceptor())
        }
    }
}
