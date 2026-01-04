package me.devnatan.dockerkt.io

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import me.devnatan.dockerkt.DockerClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

internal actual fun createHttpClient(dockerClient: DockerClient) =
    HttpClient(OkHttp) {
        configureBaseHttpClient(dockerClient)

        engine {
            preconfigured =
                OkHttpClient()
                    .newBuilder()
                    .apply { configureOkHttp(dockerClient.config.socketPath) }
                    .build()

            config {
                configureOkHttp(dockerClient.config.socketPath)
            }
        }
    }

private fun OkHttpClient.Builder.configureOkHttp(socketPath: String) {
    val isUnixSocket = isUnixSocket(socketPath)
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
