package me.devnatan.dockerkt.resource.system

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import kotlin.test.Test
import kotlin.test.fail

@ExperimentalCoroutinesApi
class SystemPingIT : ResourceIT() {
    @Test
    fun `system ping HEAD`() =
        runTest {
            runCatching {
                testClient.system.ping(head = true)
            }.onFailure { throwable ->
                fail("Failed to ping server (HEAD).", throwable)
            }
        }

    @Test
    fun `system ping GET`() =
        runTest {
            runCatching {
                testClient.system.ping(head = false)
            }.onFailure { throwable ->
                fail("Failed to ping server (GET).", throwable)
            }
        }
}
