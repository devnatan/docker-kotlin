@file:OptIn(ExperimentalCoroutinesApi::class)

package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RemoveContainerIT : ResourceIT() {
    @Test
    fun `throws ContainerNotFoundException on remove a unknown container`() =
        runTest {
            assertFailsWith<ContainerNotFoundException> {
                testClient.containers.remove("santo-bastao")
            }
        }
}
