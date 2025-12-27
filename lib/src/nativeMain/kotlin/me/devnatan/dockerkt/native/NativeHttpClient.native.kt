@file:OptIn(ExperimentalForeignApi::class)
package me.devnatan.dockerkt.native

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import me.devnatan.dockerkt.interop.HttpRequest
import me.devnatan.dockerkt.interop.HttpResponse
import me.devnatan.dockerkt.interop.http_request_execute
import me.devnatan.dockerkt.interop.http_response_free
import me.devnatan.dockerkt.interop.unix_socket_connect
import kotlin.collections.map
import kotlin.coroutines.CoroutineContext

public actual class NativeHttpEngine actual constructor(
    config: NativeHttpEngineConfig,
) : HttpClientEngine {

    actual override val config: HttpClientEngineConfig = config
    private val dnsResolver = CustomDnsResolver()

    actual override val dispatcher: CoroutineDispatcher = Dispatchers.Default

    actual override val coroutineContext: CoroutineContext
        get() = dispatcher + SupervisorJob()

    @InternalAPI
    actual override suspend fun execute(data: HttpRequestData): HttpResponseData {
        return withContext(dispatcher) {
            val host = data.url.host

            require(config is NativeHttpEngineConfig)

            if (config.customDnsServers.isNotEmpty()) {
                dnsResolver.resolve(host, config.customDnsServers).getOrThrow()
            }

            if (config.unixSocketPath != null) {
                memScoped {
                    val result = unix_socket_connect(config.unixSocketPath)
                    if (result != 0) {
                        throw Exception("Failed to connect to Unix socket: $result")
                    }
                }
            }

            val httpRequest = memScoped { prepareHttpRequest(data) }

            val response = http_request_execute(httpRequest)
                ?: throw Exception("HTTP request returned null")

            try {
                parseHttpResponse(response, data)
            } finally {
                http_response_free(response)
                freeHttpRequest(httpRequest)
            }
        }
    }

    @OptIn(InternalAPI::class)
    private fun MemScope.prepareHttpRequest(data: HttpRequestData): CPointer<HttpRequest> {
        val method = data.method.value
        val url = data.url.toString()

        val headersList = data.headers.entries().flatMap { (key, values) ->
            values.map { value -> "$key: $value" }
        }
        val headersArray = allocArrayOf(headersList.map { it.cstr.ptr })

        val bodyContent = data.body
        val (bodyPtr, bodyLen) = when (bodyContent) {
            is OutgoingContent.ByteArrayContent -> {
                val bytes = bodyContent.bytes()
                val ptr = allocArrayOf(bytes)
                ptr to bytes.size.toULong()
            }
            is OutgoingContent.ReadChannelContent -> {
                // TODO Implement ReadChannelContent
                null to 0UL
            }
            is OutgoingContent.WriteChannelContent -> {
                null to 0UL
            }
            is OutgoingContent.NoContent -> null to 0UL
            else -> null to 0UL
        }

        val request = alloc<HttpRequest>()
        request.url = url.cstr.ptr
        request.method = method.cstr.ptr
        request.headers = headersArray
        request.headers_count = headersList.size.toULong()
        request.body = bodyPtr
        request.body_len = bodyLen
        request.timeout_ms = (config as NativeHttpEngineConfig).requestTimeout.inWholeMilliseconds.toULong()

        return request.ptr
    }

    private fun parseHttpResponse(
        response: CPointer<HttpResponse>,
        requestData: HttpRequestData
    ): HttpResponseData {
        val httpResponse = response.pointed

        if (httpResponse.error != null) {
            val errorMsg = httpResponse.error!!.toKString()
            throw Exception("HTTP error: $errorMsg")
        }

        val statusCode = HttpStatusCode.fromValue(httpResponse.status_code.toInt())
        val body = httpResponse.body?.toKString() ?: ""

        return HttpResponseData(
            statusCode = statusCode,
            requestTime = GMTDate(),
            headers = Headers.Empty,
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(body.encodeToByteArray()),
            callContext = coroutineContext
        )
    }

    private fun freeHttpRequest(request: CPointer<HttpRequest>) {
    }

    actual override fun close() {
    }
}

public actual val NativeHttpEngineFactory: HttpClientEngineFactory<NativeHttpEngineConfig> = object : HttpClientEngineFactory<NativeHttpEngineConfig> {
    override fun create(block: NativeHttpEngineConfig.() -> Unit): HttpClientEngine {
        val config = NativeHttpEngineConfig().apply(block)
        return NativeHttpEngine(config)
    }
}