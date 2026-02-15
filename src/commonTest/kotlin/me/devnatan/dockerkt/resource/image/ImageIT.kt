package me.devnatan.dockerkt.resource.image

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withImage
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ImageIT : ResourceIT() {
    @Test
    fun `list images`() =
        runTest {
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
    fun `image pull access denied image`() =
        runTest {
            assertFailsWith<ImagePullDeniedException> {
                testClient.images.pull("inexistent:image").collect()
            }
        }

    @Test
    fun `image pull unknown image`() =
        runTest {
            try {
                testClient.images.remove("busybox:billiejean", force = true)
            } catch (_: ImageNotFoundException) {
            }

            assertFailsWith<ImageNotFoundException> {
                testClient.images.pull("busybox:billiejean").collect()
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

    @Test
    fun `image inspect`() =
        runTest {
            testClient.withImage("itzg/minecraft-server:latest") { imageTag ->
                val info = testClient.images.inspect(imageTag)
                assertNotNull(info)
            }
        }
}
