package me.devnatan.dockerkt.resource.image

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withImage
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class ImageIT : ResourceIT() {

    @Test
    fun `list images`() = runTest {
        testClient.images.list()
    }

    @Test
    fun `image pull`() =
        runTest {
            testClient.withImage("busybox:latest") { imageTag ->
                assertTrue(
                    actual = testClient.images.list().any { it.repositoryTags.any { repoTag -> repoTag == imageTag } },
                    message = "Pulled image must be in the images list",
                )
            }
        }

    @Test
    fun `image remove`() =
        runTest {
            val image = "busybox:latest"

            try {
                testClient.images.pull(image).collect()
            } catch (e: Throwable) {
                fail("Failed to pull image", e)
            }

            testClient.images.remove(image)
        }
}