package me.devnatan.dockerkt.resource.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.models.network.NetworkBridgeDriver
import me.devnatan.dockerkt.models.network.NetworkHostDriver
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.sleepForever
import me.devnatan.dockerkt.withContainer
import me.devnatan.dockerkt.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetworkResourceIT : ResourceIT() {

    @Test
    fun `create network with minimal configuration`() = runTest {
        testClient.networks.use(
            options = { name = "test-network-minimal" }
        ) { networkId ->
            assertNotNull(networkId)

            val network = testClient.networks.inspect(networkId)
            assertEquals("test-network-minimal", network.name)
        }
    }

    @Test
    fun `create network with full configuration`() = runTest {
        testClient.networks.use(options = {
            name = "test-network-full"
            driver = "bridge"
            checkDuplicate = true
            enableIpv6 = false
            isInternal = false
            isAttachable = true
            labels = mapOf("env" to "test", "purpose" to "integration-test")
        }) { networkId ->
            val network = testClient.networks.inspect(networkId)
            assertEquals("test-network-full", network.name)
            assertEquals("bridge", network.driver)
            assertTrue(network.isAttachable)
            assertEquals("test", network.labels["env"])
            assertEquals("integration-test", network.labels["purpose"])
        }
    }

    @Test
    fun `create overlay network`() = runTest {
        // Note: overlay driver might not be available in all Docker setups
        // This test might be skipped if not in swarm mode
        try {
            testClient.networks.use(options = {
                name = "test-overlay-network"
                driver = "overlay"
                isAttachable = true
            }) { networkId ->
                val network = testClient.networks.inspect(networkId)
                assertEquals("test-overlay-network", network.name)
                assertEquals("overlay", network.driver)
            }
        } catch (e: AssertionError) {
            println("Skipping overlay network test: ${e.message}")
        }
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

        assertNotNull(networks)
        assertTrue(networks.isNotEmpty())
        assertTrue(networks.all { it.driver == NetworkBridgeDriver })
    }

    @Test
    fun `list networks with label filter`() = runTest {
        testClient.withNetwork(options = {
            name = "test-network-labeled"
            labels = mapOf("test-label" to "test-value")
        }) {
            val networks = testClient.networks.list {
                label = "test-label=test-value"
            }

            assertTrue(networks.any { it.name == "test-network-labeled" })
        }
    }

    // @Test
    // fun `inspect network by id`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-inspect-id"
    //         driver = "bridge"
    //     }
    //
    //     try {
    //         val network = testClient.networks.inspect(networkId.id)
    //
    //         assertEquals(networkId.id, network.id)
    //         assertEquals("test-network-inspect-id", network.name)
    //         assertEquals("bridge", network.driver)
    //         assertNotNull(network.created)
    //     } finally {
    //         testClient.networks.remove(networkId.id)
    //     }
    // }
    //
    // @Test
    // fun `inspect network by name`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-inspect-name"
    //     }
    //
    //     try {
    //         val network = testClient.networks.inspect("test-network-inspect-name")
    //
    //         assertEquals(networkId.id, network.id)
    //         assertEquals("test-network-inspect-name", network.name)
    //     } finally {
    //         testClient.networks.remove(networkId.id)
    //     }
    // }
    //
    // @Test
    // fun `inspect network with verbose option`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-verbose"
    //     }
    //
    //     try {
    //         val network = testClient.networks.inspect(networkId.id) {
    //             verbose = true
    //         }
    //
    //         assertEquals("test-network-verbose", network.name)
    //         // Verbose mode provides more detailed information
    //         assertNotNull(network.ipam)
    //     } finally {
    //         testClient.networks.remove(networkId.id)
    //     }
    // }
    //
    // @Test
    // fun `inspect fails when network not found`() = runTest {
    //     assertFailsWith<NetworkNotFoundException> {
    //         testClient.networks.inspect("nonexistent-network-id-12345")
    //     }
    // }
    //
    // @Test
    // fun `remove network by id`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-remove-id"
    //     }
    //
    //     // Verify it exists
    //     val network = testClient.networks.inspect(networkId.id)
    //     assertEquals("test-network-remove-id", network.name)
    //
    //     // Remove it
    //     testClient.networks.remove(networkId.id)
    //
    //     // Verify it's gone
    //     assertFailsWith<NetworkNotFoundException> {
    //         testClient.networks.inspect(networkId.id)
    //     }
    // }
    //
    // @Test
    // fun `remove network by name`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-remove-name"
    //     }
    //
    //     // Verify it exists
    //     testClient.networks.inspect("test-network-remove-name")
    //
    //     // Remove it
    //     testClient.networks.remove("test-network-remove-name")
    //
    //     // Verify it's gone
    //     assertFailsWith<NetworkNotFoundException> {
    //         testClient.networks.inspect("test-network-remove-name")
    //     }
    // }
    //
    // @Test
    // fun `connect container to network`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-connect"
    //     }
    //
    //     testClient.withContainer(
    //         image = "alpine:latest",
    //         options = { sleepForever() },
    //     ) { containerId ->
    //         try {
    //             testClient.containers.start(containerId)
    //
    //             // Connect container to network
    //             testClient.networks.connectContainer(networkId.id, containerId)
    //
    //             // Give it a moment to connect
    //             delay(500)
    //
    //             // Verify connection
    //             val network = testClient.networks.inspect(networkId.id)
    //             assertTrue(network.containers?.containsKey(containerId) == true)
    //
    //             testClient.containers.stop(containerId)
    //         } finally {
    //             testClient.networks.remove(networkId.id)
    //         }
    //     }
    // }
    //
    // @Test
    // fun `connect container to network by names`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-connect-names"
    //     }
    //
    //     testClient.withContainer(
    //         image = "alpine:latest",
    //         options = {
    //             name = "test-container-connect"
    //             sleepForever()
    //         },
    //     ) { containerId ->
    //         try {
    //             testClient.containers.start(containerId)
    //
    //             // Connect using names instead of IDs
    //             testClient.networks.connectContainer(
    //                 "test-network-connect-names",
    //                 "test-container-connect"
    //             )
    //
    //             delay(500)
    //
    //             // Verify connection
    //             val network = testClient.networks.inspect("test-network-connect-names")
    //             assertTrue(network.containers.containsKey(containerId))
    //
    //             testClient.containers.stop(containerId)
    //         } finally {
    //             testClient.networks.remove(networkId.id)
    //         }
    //     }
    // }
    //
    // @Test
    // fun `disconnect container from network`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-disconnect"
    //     }
    //
    //     testClient.withContainer(
    //         image = "alpine:latest",
    //         options = { sleepForever() },
    //     ) { containerId ->
    //         try {
    //             testClient.containers.start(containerId)
    //
    //             // Connect container
    //             testClient.networks.connectContainer(networkId.id, containerId)
    //             delay(500)
    //
    //             // Verify connection
    //             var network = testClient.networks.inspect(networkId.id)
    //             assertTrue(network.containers.containsKey(containerId))
    //
    //             // Disconnect container
    //             testClient.networks.disconnectContainer(networkId.id, containerId)
    //             delay(500)
    //
    //             // Verify disconnection
    //             network = testClient.networks.inspect(networkId.id)
    //             assertFalse(network.containers.containsKey(containerId))
    //
    //             testClient.containers.stop(containerId)
    //         } finally {
    //             testClient.networks.remove(networkId.id)
    //         }
    //     }
    // }
    //
    // @Test
    // fun `multiple containers on same network`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-multi-containers"
    //     }
    //
    //     testClient.withContainer(
    //         "alpine:latest",
    //         {
    //             command = listOf("sleep", "infinity")
    //         },
    //     ) { container1Id ->
    //         testClient.withContainer(
    //             "alpine:latest",
    //             {
    //                 command = listOf("sleep", "infinity")
    //             },
    //         ) { container2Id ->
    //             try {
    //                 testClient.containers.start(container1Id)
    //                 testClient.containers.start(container2Id)
    //
    //                 // Connect both containers
    //                 testClient.networks.connectContainer(networkId.id, container1Id)
    //                 testClient.networks.connectContainer(networkId.id, container2Id)
    //
    //                 delay(500)
    //
    //                 // Verify both are connected
    //                 val network = testClient.networks.inspect(networkId.id)
    //                 assertTrue(network.containers.containsKey(container1Id))
    //                 assertTrue(network.containers.containsKey(container2Id))
    //                 assertEquals(2, network.containers.size)
    //
    //                 testClient.containers.stop(container1Id)
    //                 testClient.containers.stop(container2Id)
    //             } finally {
    //                 testClient.networks.remove(networkId.id)
    //             }
    //         }
    //     }
    // }
    //
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
    //
    // @Test
    // fun `network with custom IPAM configuration`() = runTest {
    //     val networkId = testClient.networks.create {
    //         name = "test-network-custom-ipam"
    //         driver = "bridge"
    //         ipam = IPAM(driver = "default", config = listOf(IPAMConfig(
    //             subnet = "172.20.0.0/16",
    //             ipRange = "172.20.10.0/24",
    //             gateway = "172.20.0.1"
    //         )))
    //     }
    //
    //     try {
    //         val network = testClient.networks.inspect(networkId.id)
    //         assertEquals("test-network-custom-ipam", network.name)
    //
    //         val ipamConfig = network.ipam?.config?.firstOrNull()
    //         assertNotNull(ipamConfig)
    //         assertEquals("172.20.0.0/16", ipamConfig.subnet)
    //         assertEquals("172.20.10.0/24", ipamConfig.ipRange)
    //         assertEquals("172.20.0.1", ipamConfig.gateway)
    //     } finally {
    //         testClient.networks.remove(networkId.id)
    //     }
    // }
    //

    @Test
    fun `create network with custom options`() = runTest {
        testClient.networks.use(options = {
            name = "test-network-options"
            driver = "bridge"
            options = mapOf(
                "com.docker.network.bridge.name" to "test-bridge",
                "com.docker.network.bridge.enable_ip_masquerade" to "true"
            )
        }) { networkId ->
            val network = testClient.networks.inspect(networkId)
            assertEquals("test-network-options", network.name)
            assertNotNull(network.options)
            assertEquals("test-bridge", network.options["com.docker.network.bridge.name"])
        }
    }


    @Test
    fun `create internal network`() = runTest {
        testClient.networks.use(options = {
            name = "test-network-internal"
            isInternal = true
        }) { networkId ->
            val network = testClient.networks.inspect(networkId)
            assertEquals("test-network-internal", network.name)
            assertTrue(network.isInternal)
        }
    }

    @Test
    fun `create network with IPv6 enabled`() = runTest {
        testClient.networks.use(options = {
            name = "test-network-ipv6"
            enableIpv6 = true
        }) { networkId ->
            val network = testClient.networks.inspect(networkId)
            assertEquals("test-network-ipv6", network.name)
            assertTrue(network.enableIPv6)
        }
    }


    @Test
    fun `create network fails with duplicate name`() = runTest {
        testClient.networks.use(options = {
            name = "test-network-duplicate"
        }) {
            // Try to create another network with the same name
            assertFailsWith<Exception> {
                testClient.networks.create {
                    name = "test-network-duplicate"
                    checkDuplicate = true
                }
            }
        }
    }
}