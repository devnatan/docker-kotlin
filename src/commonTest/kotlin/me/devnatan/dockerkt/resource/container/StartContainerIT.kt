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

class StartContainerIT : ResourceIT() {
    @Test
    fun `start a container`() =
        runTest {
            testClient.withContainer("busybox:latest") { id ->
                testClient.containers.start(id)
            }
        }

    @Test
    fun `start a container with auto-assigned port bindings`() =
        runTest {
            testClient.withContainer(
                "nginx:latest",
                {
                    exposedPort(80u)
                    hostConfig {
                        portBindings(80u)
                    }
                },
            ) { id ->
                testClient.containers.start(id)

                val container = testClient.containers.inspect(id)
                val ports = container.networkSettings.ports
                assertTrue { ports.isNotEmpty() }

                val exposedPort = ExposedPort(80u, ExposedPortProtocol.TCP)
                assertContains(map = ports, key = exposedPort)

                val port80Bindings = container.networkSettings.ports[exposedPort]
                assertNotNull(port80Bindings)
                assertTrue { port80Bindings.isNotEmpty() }

                val ipv4Binding = port80Bindings[0]
                assertEquals(ipv4Binding.ip, "0.0.0.0")
                assertNotNull(ipv4Binding.port)
                assertTrue { ipv4Binding.port!!.toInt() > 0 }

                if (port80Bindings.size > 1) {
                    val ipv6Binding = port80Bindings[1]
                    assertEquals(ipv6Binding.ip, "::")
                    assertNotNull(ipv6Binding.port)
                    assertTrue { ipv6Binding.port!!.toInt() > 0 }
                }
            }
        }

    @Test
    fun `throws ContainerNotFoundException on start unknown container`() =
        runTest {
            assertFailsWith<ContainerNotFoundException> {
                // TODO generate random id
                testClient.containers.start("jao-gomides")
            }
        }

    @Test
    fun `throws ContainerAlreadyStartedException on start a already started container`() =
        runTest {
            testClient.withContainer(
                image = "busybox:latest",
                options = { keepStartedForever() },
            ) { container ->
                testClient.containers.start(container)

                assertFailsWith<ContainerAlreadyStartedException> {
                    testClient.containers.start(container)
                }
            }
        }
}
