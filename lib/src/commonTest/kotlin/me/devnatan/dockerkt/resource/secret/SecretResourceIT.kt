@file:OptIn(ExperimentalCoroutinesApi::class)

package me.devnatan.dockerkt.resource.secret

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.resource.swarm.NodeNotPartOfSwarmException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SecretResourceIT : ResourceIT() {
    @Test
    fun `list secrets with no swarm`() =
        runTest {
            assertFailsWith<NodeNotPartOfSwarmException> {
                testClient.secrets.list()
            }
        }
}
