package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import me.devnatan.dockerkt.models.container.ContainerStatsResult
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.sleepForever
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class StatsContainerIT : ResourceIT() {
    @Test
    fun `stats from non-existent container throws ContainerNotFoundException`() =
        runTest {
            assertFailsWith<ContainerNotFoundException> {
                testClient.containers.stats("non-existent-container-12345") {
                    stream = false
                    oneShot = true
                }
            }
        }

    @Test
    fun `single stats snapshot returns one reading`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { sleepForever() },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    withTimeout(15.seconds) {
                        testClient.containers.stats(container) {
                            stream = false
                            oneShot = true
                        }
                    }

                assertIs<ContainerStatsResult.Single>(result)
                assertNotNull(result.output.read)
                assertTrue(result.output.read.isNotBlank(), "read timestamp should be set")
            }
        }

    @Test
    fun `streaming stats emits multiple readings`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { sleepForever() },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.stats(container) {
                        stream = true
                    }

                assertIs<ContainerStatsResult.Stream>(result)

                val readings =
                    withTimeout(30.seconds) {
                        result.output.take(2).toList()
                    }

                assertEquals(2, readings.size, "Expected 2 stats readings")
                readings.forEach { reading ->
                    assertTrue(reading.read.isNotBlank(), "read timestamp should be set")
                }
            }
        }

    @Test
    fun `streaming stats can be cancelled with first`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { sleepForever() },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.stats(container) {
                        stream = true
                    }

                assertIs<ContainerStatsResult.Stream>(result)

                val reading =
                    withTimeout(15.seconds) {
                        result.output.first()
                    }

                assertTrue(reading.read.isNotBlank())
            }
        }
}
