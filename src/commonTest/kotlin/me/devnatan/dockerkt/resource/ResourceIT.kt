package me.devnatan.dockerkt.resource

import me.devnatan.dockerkt.DockerClient
import me.devnatan.dockerkt.createTestDockerClient
import kotlin.test.AfterTest

open class ResourceIT(
    private val debugHttpCalls: Boolean = true,
) {
    val testClient: DockerClient by lazy {
        createTestDockerClient {
            debugHttpCalls(this@ResourceIT.debugHttpCalls)
        }
    }

    @AfterTest
    fun closeTestClient() {
        runCatching { testClient.close() }
    }
}
