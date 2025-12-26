package me.devnatan.dockerkt.resource.container

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withContainer
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LogContainerIT : ResourceIT() {
    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun `stream container logs`() =
        runTest {
            testClient.withContainer("nginx:latest") { containerId ->
                testClient.containers.start(containerId)

                val completed = CompletableDeferred<Boolean>()

                launch {
                    testClient.containers.logs(containerId).collect {
                        completed.complete(true)
                        cancel()
                    }
                }

                completed.await()
                assertTrue(completed.getCompleted())
            }
        }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `stream stopped container logs`() =
        runTest {
            testClient.withContainer("nginx:latest") { containerId ->
                assertFailsWith<CancellationException>(
                    message = "Container stopped? Logs are not available.",
                ) {
                    testClient.containers.logs(containerId).collect()
                }
            }
        }

    @Test
    fun `stream container logs interrupted`() =
        runTest {
            testClient.withContainer("nginx:latest") { containerId ->
                testClient.containers.start(containerId)

                val frames = mutableListOf<me.devnatan.dockerkt.models.Frame>()

                launch {
                    delay(3.seconds)

                    // Force container stop so we can catch EOFException
                    testClient.containers.stop(containerId)
                }

                testClient.containers.logs(containerId).collect(frames::add)

                assertTrue(frames.isNotEmpty())
            }
        }
}
