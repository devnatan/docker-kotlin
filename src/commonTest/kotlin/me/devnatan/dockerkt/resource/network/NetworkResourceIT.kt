package me.devnatan.dockerkt.resource.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.models.network.NetworkBridgeDriver
import me.devnatan.dockerkt.models.network.NetworkHostDriver
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.sleepForever
import me.devnatan.dockerkt.withContainer
import me.devnatan.dockerkt.withNetwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetworkResourceIT : ResourceIT() {
    @Test
    fun `create network`() =
        runTest {
            val createdNetwork = testClient.networks.create { name = "dockerkt" }
            val inspectedNetwork = testClient.networks.inspect(createdNetwork.id)
            assertEquals(createdNetwork.id, inspectedNetwork.id)

            // cleanup
            testClient.networks.remove(inspectedNetwork.id)
        }

    @Test
    fun `remove network`() =
        runTest {
            val network = testClient.networks.create { name = "dockerkt" }
            assertTrue(testClient.networks.list().any { it.id == network.id })

            testClient.networks.remove(network.id)
            assertTrue(testClient.networks.list().none { it.id == network.id })
        }

    @Test
    fun `list networks`() =
        runTest {
            // the list of networks will never be empty because Docker
            // has predefined networks that cannot be removed
            assertFalse(testClient.networks.list().isEmpty())
        }

    @Test
    fun `prune networks`() =
        runTest {
            val oldCount = testClient.networks.list().size
            val newCount = 5
            repeat(newCount) {
                testClient.networks.create { name = "dockerkt-$it" }
            }

            // check for >= because docker can have default networks defined
            assertEquals(testClient.networks.list().size, oldCount + newCount)

            // just ensure prune will work correctly, comparing sizes may not
            // work well in different environments
            testClient.networks.prune()
        }
}
    @Test
    fun `prune unused networks`() = runTest {
        try {
            val network1 = testClient.networks.create { name = "test-network-prune-1" }
            val network2 = testClient.networks.create { name = "test-network-prune-2" }

            testClient.networks.inspect(network1)
            testClient.networks.inspect(network2)

            testClient.networks.prune()

            delay(500)

            // Networks should be removed (they were unused)
            assertFailsWith<NetworkNotFoundException> {
                testClient.networks.inspect(network1)
            }
            assertFailsWith<NetworkNotFoundException> {
                testClient.networks.inspect(network2)
            }
        } finally {
            // Suppress errors for removal
            runCatching { testClient.networks.remove("test-network-prune-1") }
            runCatching { testClient.networks.remove("test-network-prune-2") }
        }
    }

    @Test
    fun `prune does not remove networks with connected containers`() = runTest {
        testClient.withNetwork(options = {
            name = "test-network-prune-with-container"
        }) { networkId ->
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    sleepForever()
                },
            ) { containerId ->
                testClient.containers.start(containerId)

                // Connect container to network
                testClient.networks.connectContainer(networkId, containerId)
                delay(500)

                // Try to prune - this network should NOT be removed
                testClient.networks.prune()
                delay(500)

                val network = testClient.networks.inspect(networkId)
                assertEquals(
                    expected = "test-network-prune-with-container",
                    actual = network.name,
                    message = "Network should still exist because it has a connected container"
                )
            }
        }
    }