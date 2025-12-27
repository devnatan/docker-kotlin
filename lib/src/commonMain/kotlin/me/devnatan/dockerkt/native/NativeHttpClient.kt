package me.devnatan.dockerkt.native

import io.ktor.client.engine.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class NativeHttpEngineConfig : HttpClientEngineConfig() {
    public var customDnsServers: List<String> = emptyList()
    public var unixSocketPath: String? = null
    public var requestTimeout: Duration = 5.seconds
}

public expect class NativeHttpEngine(config: NativeHttpEngineConfig) : HttpClientEngine {
    override val dispatcher: CoroutineDispatcher
    override val config: HttpClientEngineConfig

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData
    override val coroutineContext: CoroutineContext
    override fun close()
}

public expect val NativeHttpEngineFactory: HttpClientEngineFactory<NativeHttpEngineConfig>