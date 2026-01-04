@file:OptIn(ExperimentalCoroutinesApi::class)

package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.keepStartedForever
import me.devnatan.dockerkt.models.container.volume
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InspectContainerIT : ResourceIT() {
    @Test
    fun `inspects container with volumes`() =
        runTest {
            testClient.withContainer(
                image = "busybox:latest",
                options = {
                    volume("/opt")
                    keepStartedForever()
                },
            ) { containerId ->
                val container = testClient.containers.inspect(containerId)
                val volumes = container.config.volumes
                assertEquals(volumes, listOf("/opt"))
            }
        }
}
