package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import me.devnatan.dockerkt.models.Frame
import me.devnatan.dockerkt.models.container.ContainerLogsResult
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LogContainerIT : ResourceIT() {
    @Test
    fun `logs from non-existent container throws ContainerNotFoundException`() =
        runTest {
            assertFailsWith<ContainerNotFoundException> {
                testClient.containers.logs("non-existent-container-12345") {
                    stdout = true
                }
            }
        }

    @Test
    fun `get logs from container with stdout`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command = listOf("sh", "-c", "echo 'Hello from stdout'")
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                        stderr = false
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertContains(result.output, "Hello from stdout")
            }
        }

    @Test
    fun `get logs from container with stderr`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command = listOf("sh", "-c", "echo 'Error message' >&2")
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = false
                        stderr = true
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertContains(result.output, "Error message")
            }
        }

    @Test
    fun `get logs with both stdout and stderr`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command =
                        listOf(
                            "sh",
                            "-c",
                            "echo 'stdout message' && echo 'stderr message' >&2",
                        )
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                        stderr = true
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertContains(result.output, "stdout message")
                assertContains(result.output, "stderr message")
            }
        }

    @Test
    fun `get logs with demux separates stdout and stderr`() =
        runTest {
            testClient.withContainer(
                "alpine:latest",
                {
                    command =
                        listOf(
                            "sh",
                            "-c",
                            "echo 'out1' && echo 'err1' >&2 && echo 'out2' && echo 'err2' >&2",
                        )
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        demux = true
                        stdout = true
                        stderr = true
                    }

                assertIs<ContainerLogsResult.CompleteDemuxed>(result)
                assertContains(result.stdout, "out1")
                assertContains(result.stdout, "out2")
                assertContains(result.stderr, "err1")
                assertContains(result.stderr, "err2")
            }
        }

    @Test
    fun `logs from TTY-enabled container`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command = listOf("sh", "-c", "echo 'TTY output'")
                    tty = true
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                    }

                // TTY mode returns Complete (not demuxed) or Stream
                when (result) {
                    is ContainerLogsResult.Complete -> {
                        assertContains(result.output, "TTY output")
                    }

                    is ContainerLogsResult.Stream -> {
                        val content = result.output.toList().joinToString("")
                        assertContains(content, "TTY output")
                    }

                    else -> {
                        throw AssertionError("Unexpected result type: $result")
                    }
                }
            }
        }

    @Test
    fun `logs with multi-line output`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command =
                        listOf(
                            "sh",
                            "-c",
                            "echo -e 'Line 1\\nLine 2\\nLine 3'",
                        )
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertContains(result.output, "Line 1")
                assertContains(result.output, "Line 2")
                assertContains(result.output, "Line 3")
            }
        }

    @Test
    fun `logs with unicode characters`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command = listOf("sh", "-c", "echo 'Hello 世界 🌍'")
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertContains(result.output, "Hello 世界 🌍")
            }
        }

    @Test
    fun `logs with empty output`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    command = listOf("sh", "-c", "true") // No output
                },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertEquals("", result.output)
            }
        }

    @Test
    fun `logs with large output`() =
        runTest {
            testClient.withContainer(
                image = "alpine:latest",
                options = {
                    // Generate 1000 lines of output
                    command =
                        listOf(
                            "sh",
                            "-c",
                            $$"for i in $(seq 1 1000); do echo \"Log line $i with some padding text to make it longer\"; done",
                        )
                },
            ) { container ->
                testClient.containers.start(container)

                delay(3000)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                    }

                assertIs<ContainerLogsResult.Complete>(result)
                assertContains(result.output, "Log line 1")
                assertContains(result.output, "Log line 1000")
            }
        }

    @Test
    fun `stream logs with follow enabled`() =
        runBlocking {
            testClient.withContainer(
                "alpine:latest",
                {
                    command =
                        listOf(
                            "sh",
                            "-c",
                            $$"for i in 1 2 3; do echo \"Stream $i\"; sleep 0.5; done",
                        )
                },
            ) { id ->
                testClient.containers.start(id)

                delay(200)

                val result =
                    testClient.containers.logs(id) {
                        stdout = true
                        follow = true
                    }

                assertIs<ContainerLogsResult.Stream>(result)

                val framesChannel = Channel<Frame>(Channel.UNLIMITED)

                val collectorJob =
                    launch {
                        result.output.collect { frame ->
                            framesChannel.send(frame)
                        }
                        framesChannel.close()
                    }

                val frames = mutableListOf<Frame>()
                val collectJob =
                    launch {
                        for (frame in framesChannel) {
                            frames.add(frame)
                            if (frames.size >= 3) break
                        }
                    }

                withTimeoutOrNull(15.seconds) {
                    collectJob.join()
                }

                collectorJob.cancelAndJoin()

                assertTrue(frames.isNotEmpty(), "Should have collected at least one frame")
                val allContent = frames.joinToString("") { it.value }
                assertTrue(allContent.contains("Stream"), "Should contain 'Stream' in output")
            }
        }

    @Test
    fun `stream logs collects until container stops`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { command = listOf("sh", "-c", "echo 'Start'; sleep 1; echo 'End'") },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                        follow = true
                    }

                assertIs<ContainerLogsResult.Stream>(result)

                val frames =
                    withTimeoutOrNull(10.seconds) {
                        result.output.toList()
                    }

                assertNotNull(frames)
                val allContent = frames.joinToString("") { it.value }
                assertContains(allContent, "Start")
                assertContains(allContent, "End")
            }
        }

    @Test
    fun `stream logs can be cancelled with take`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { command = listOf("sh", "-c", "while true; do echo 'tick'; sleep 0.2; done") },
            ) { container ->
                testClient.containers.start(container)

                // Small delay to ensure output starts
                delay(300)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                        follow = true
                    }

                assertIs<ContainerLogsResult.Stream>(result)

                // Take only 3 frames
                val frames =
                    withTimeoutOrNull(10.seconds) {
                        result.output.take(3).toList()
                    }

                assertNotNull(frames, "Should have collected frames")
                assertEquals(3, frames.size, "Should have exactly 3 frames")
            }
        }

    @Test
    fun `stream logs can be manually cancelled`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { command = listOf("sh", "-c", "while true; do echo 'tick'; sleep 0.2; done") },
            ) { container ->
                testClient.containers.start(container)
                delay(300)

                val result =
                    testClient.containers.logs(container) {
                        stdout = true
                        follow = true
                    }

                assertIs<ContainerLogsResult.Stream>(result)

                val frames = mutableListOf<Frame>()

                val job =
                    launch {
                        result.output.collect { frame ->
                            frames.add(frame)
                        }
                    }

                delay(1.seconds)

                job.cancelAndJoin()

                assertTrue(frames.isNotEmpty(), "Should have collected frames before cancel")
            }
        }

    @Test
    fun `stream logs with demux`() =
        runBlocking {
            testClient.withContainer(
                image = "alpine:latest",
                options = { command = listOf("sh", "-c", "echo 'out' && echo 'err' >&2") },
            ) { container ->
                testClient.containers.start(container)

                val result =
                    testClient.containers.logs(container) {
                        demux = true
                        stdout = true
                        stderr = true
                        follow = true
                    }

                assertIs<ContainerLogsResult.StreamDemuxed>(result)

                val stdout = StringBuilder()
                val stderr = StringBuilder()

                val stdoutJob =
                    launch {
                        result.stdout.collect { stdout.append(it) }
                    }
                val stderrJob =
                    launch {
                        result.stderr.collect { stderr.append(it) }
                    }

                withTimeoutOrNull(10.seconds) {
                    stdoutJob.join()
                    stderrJob.join()
                }

                stdoutJob.cancelAndJoin()
                stderrJob.cancelAndJoin()

                assertTrue(
                    stdout.isNotEmpty() || stderr.isNotEmpty(),
                    "Should have collected stdout or stderr",
                )
            }
        }
}
