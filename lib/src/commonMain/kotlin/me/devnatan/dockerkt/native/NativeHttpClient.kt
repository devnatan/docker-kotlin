package me.devnatan.dockerkt.native

import io.ktor.client.engine.*

public class NativeHttpEngineConfig : HttpClientEngineConfig() {
    public var customDnsServers: List<String> = emptyList()
    public var unixSocketPath: String? = null
}

public expect abstract class NativeHttpEngine(config: NativeHttpEngineConfig) : HttpClientEngine

public expect val NativeHttpEngineFactory: HttpClientEngineFactory<NativeHttpEngineConfig>