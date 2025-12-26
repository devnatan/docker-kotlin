@file:OptIn(ExperimentalCoroutinesApi::class)

package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.models.container.volume
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class InspectContainerIT : ResourceIT() {
    @Test
    fun `inspects container with volumes`() =
        runTest {
            testClient.withContainer(
                "busybox:latest",
                {
                    volume("/opt")
                    command = listOf("sleep", "infinity")
                },
            ) { id ->
                testClient.containers.start(id)
                try {
                    val container = testClient.containers.inspect(id)
                    val volumes = container.config.volumes
                    assertEquals(volumes, listOf("/opt"))
                } finally {
                    testClient.containers.stop(id)
                }
            }
        }
}
