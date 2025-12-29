package me.devnatan.dockerkt.resource.exec

import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import me.devnatan.dockerkt.models.exec.ExecStartOptions
import me.devnatan.dockerkt.models.exec.ExecStartResult
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.resource.container.ContainerNotRunningException
import me.devnatan.dockerkt.sleepForever
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val TestImage = "alpine:latest"

class ExecContainerIT : ResourceIT() {
    @Test
    fun `exec a command in detached mode`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("true")
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(detach = true),
                    )

                assertTrue(result is ExecStartResult.Detached)

                delay(100)

                val exec = testClient.exec.inspect(execId)
                assertEquals(0, exec.exitCode)

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a failing command in detached mode`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("false")
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(detach = true),
                    )

                assertTrue(result is ExecStartResult.Detached)

                delay(100)

                val exec = testClient.exec.inspect(execId)
                assertEquals(1, exec.exitCode)

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a command and capture complete output`() =
        runTest {
            testClient.withContainer(
                TestImage,
                {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("sh", "-c", "echo 'hello world'")
                        attachStdout = true
                        attachStderr = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(detach = false, stream = false),
                    )

                assertTrue(result is ExecStartResult.Complete)
                val output = result.output
                assertTrue(output.contains("hello world"))

                val exec = testClient.exec.inspect(execId)
                assertEquals(0, exec.exitCode)

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a command with streaming output`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("sh", "-c", "echo line1 && echo line2 && echo line3")
                        attachStdout = true
                        attachStderr = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(stream = true),
                    )

                assertTrue(result is ExecStartResult.Stream)

                val output =
                    buildString {
                        result.output.collect { chunk ->
                            append(chunk)
                        }
                    }

                assertTrue(output.contains("line1"), "Output should contain 'line1', but was: $output")
                assertTrue(output.contains("line2"), "Output should contain 'line2', but was: $output")
                assertTrue(output.contains("line3"), "Output should contain 'line3', but was: $output")

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a command with demuxed output`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command =
                            listOf(
                                "sh",
                                "-c",
                                "echo 'stdout message' && echo 'stderr message' >&2",
                            )
                        attachStdout = true
                        attachStderr = true
                        tty = false
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(demux = true, tty = false),
                    )

                assertTrue(result is ExecStartResult.CompleteDemuxed)
                val output = result.output

                assertTrue(output.stdout.contains("stdout message"), "stdout was: ${output.stdout}")
                assertTrue(output.stderr.contains("stderr message"), "stderr was: ${output.stderr}")

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a command with demuxed streaming output`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command =
                            listOf(
                                "sh",
                                "-c",
                                "echo out1 && echo err1 >&2 && echo out2 && echo err2 >&2",
                            )
                        attachStdout = true
                        attachStderr = true
                        tty = false
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(stream = true, demux = true, tty = false),
                    )

                assertTrue(result is ExecStartResult.StreamDemuxed)

                val stdout = StringBuilder()
                val stderr = StringBuilder()

                result.output.collect { output ->
                    stdout.append(output.stdout)
                    stderr.append(output.stderr)
                }

                val stdoutStr = stdout.toString()
                val stderrStr = stderr.toString()

                assertTrue(stdoutStr.contains("out1"), "stdout was: $stdoutStr")
                assertTrue(stdoutStr.contains("out2"), "stdout was: $stdoutStr")
                assertTrue(stderrStr.contains("err1"), "stderr was: $stderrStr")
                assertTrue(stderrStr.contains("err2"), "stderr was: $stderrStr")

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a command with TTY enabled`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("sh", "-c", "echo 'interactive output'")
                        attachStdout = true
                        attachStderr = true
                        tty = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(tty = true, stream = false),
                    )

                assertTrue(result is ExecStartResult.Complete)
                assertTrue(result.output.contains("interactive output"))

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec a command with socket mode`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("sh", "-c", "echo 'socket test'")
                        attachStdout = true
                        attachStderr = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(socket = true),
                    )

                assertTrue(result is ExecStartResult.Socket)

                val channel = result.channel
                try {
                    val buffer = ByteArray(1024)
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead > 0) {
                        val output = buffer.copyOf(bytesRead).decodeToString()
                        assertTrue(output.contains("socket test"))
                    }
                } finally {
                    channel.cancel()
                }

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec long running command with streaming`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command =
                            listOf(
                                "sh",
                                "-c",
                                "for i in 1 2 3 4 5; do echo \"line \$i\"; sleep 0.1; done",
                            )
                        attachStdout = true
                        attachStderr = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(stream = true),
                    )

                assertTrue(result is ExecStartResult.Stream)

                val chunks = mutableListOf<String>()
                result.output.collect { chunk ->
                    chunks.add(chunk)
                }

                val fullOutput = chunks.joinToString("")
                assertTrue(fullOutput.contains("line 1"))
                assertTrue(fullOutput.contains("line 5"))

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec create fails when container is not running`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    command = listOf("true")
                },
            ) { id ->
                assertFailsWith<ContainerNotRunningException> {
                    testClient.exec.create(id) {
                        command = listOf("echo", "test")
                    }
                }
            }
        }

    @Test
    fun `exec start fails when container is not running`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("echo", "test")
                    }

                testClient.containers.stop(id)

                assertFailsWith<ExecNotFoundException> {
                    testClient.exec.start(execId)
                }
            }
        }

    @Test
    fun `exec fails when exec instance not found`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                assertFailsWith<ExecNotFoundException> {
                    testClient.exec.start(
                        "nonexistent_exec_id",
                        ExecStartOptions(detach = true),
                    )
                }

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec with environment variables`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("sh", "-c", "echo \"\$MY_VAR\"")
                        env = listOf("MY_VAR=hello from env")
                        attachStdout = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(stream = false),
                    )

                assertTrue(result is ExecStartResult.Complete)
                assertTrue(result.output.contains("hello from env"))

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `exec with working directory`() =
        runTest {
            testClient.withContainer(
                image = TestImage,
                options = {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val execId =
                    testClient.exec.create(id) {
                        command = listOf("pwd")
                        workingDir = "/tmp"
                        attachStdout = true
                    }

                val result =
                    testClient.exec.start(
                        id = execId,
                        options = ExecStartOptions(stream = false),
                    )

                assertTrue(result is ExecStartResult.Complete)
                val output = result.output.trim()
                assertEquals("/tmp", output)

                testClient.containers.stop(id)
            }
        }

    @Test
    fun `validate mutually exclusive options`() {
        assertFailsWith<IllegalArgumentException> {
            ExecStartOptions(stream = true, socket = true)
        }
    }

    @Test
    fun `validate demux requires tty false`() {
        assertFailsWith<IllegalArgumentException> {
            ExecStartOptions(demux = true, tty = true)
        }
    }
}
