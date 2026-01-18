package me.devnatan.dockerkt.resource.system

import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.DockerResourceException
import me.devnatan.dockerkt.resource.ResourceIT
import kotlin.test.Test
import kotlin.test.fail

class SystemInfoIT : ResourceIT() {
    @Test
    fun `fetch system info`() =
        runTest {
            try {
                testClient.system.info()
            } catch (e: DockerResourceException) {
                fail("Failed to fetch information about system info.", e)
            }
        }
}
