@file:OptIn(ExperimentalCoroutinesApi::class)

package me.devnatan.dockerkt.resource.volume

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withVolume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VolumeResourceIT : ResourceIT() {
    companion object {
        const val VOLUME_NAME = "docker-kotlin"
    }

    @Test
    fun `create volume`() =
        runTest {
            testClient.withVolume { volume ->
                val inspection = testClient.volumes.inspect(volume.name)
                assertEquals(volume.name, inspection.name)
            }
        }

    @Test
    fun `remove volume`() =
        runTest {
            val volumes = testClient.volumes.create { name = VOLUME_NAME }
            assertTrue(
                testClient.volumes
                    .list()
                    .volumes
                    .any { it.name == volumes.name },
            )

            testClient.volumes.remove(volumes.name)
            assertTrue(
                testClient.volumes
                    .list()
                    .volumes
                    .none { it.name == volumes.name },
            )
        }

    @Test
    fun `list volumes`() =
        runTest {
            testClient.volumes.list().volumes
            // Just expect no exception is thrown
        }

    @Test
    fun `prune volume`() =
        runTest {
            val oldCount =
                testClient.volumes
                    .list()
                    .volumes.size

            val newCount = 5
            repeat(newCount) {
                testClient.volumes.create()
            }

            assertEquals(
                testClient.volumes
                    .list()
                    .volumes.size,
                oldCount + newCount,
            )

            testClient.volumes.prune()
            assertEquals(
                testClient.volumes
                    .list()
                    .volumes.size,
                oldCount,
            )
        }
}
