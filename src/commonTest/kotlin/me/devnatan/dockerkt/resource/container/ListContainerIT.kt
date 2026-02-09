@file:OptIn(ExperimentalCoroutinesApi::class)

package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.keepStartedForever
import me.devnatan.dockerkt.models.ExposedPort
import me.devnatan.dockerkt.models.ExposedPortProtocol
import me.devnatan.dockerkt.models.container.exposedPort
import me.devnatan.dockerkt.models.container.hostConfig
import me.devnatan.dockerkt.models.portBindings
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListContainerIT : ResourceIT() {
    @Test
    fun `list containers`() =
        runTest {
            assertNotNull(testClient.containers.list())
        }
}
