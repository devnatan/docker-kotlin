package me.devnatan.dockerkt.resource.system

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.DockerResourceException
import me.devnatan.dockerkt.resource.ResourceIT
import kotlin.test.Test
import kotlin.test.fail

@ExperimentalCoroutinesApi
class SystemVersionIT : ResourceIT() {
    @Test
    fun `fetch system version`() =
        runTest {
            try {
                testClient.system.version()
            } catch (e: DockerResourceException) {
                fail("Failed to fetch information about system version.", e)
            }
        }
}
