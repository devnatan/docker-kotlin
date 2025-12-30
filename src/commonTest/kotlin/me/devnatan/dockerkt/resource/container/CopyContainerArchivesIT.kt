package me.devnatan.dockerkt.resource.container

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import me.devnatan.dockerkt.io.FileSystemUtils
import me.devnatan.dockerkt.models.exec.ExecStartOptions
import me.devnatan.dockerkt.models.exec.ExecStartResult
import me.devnatan.dockerkt.resource.ResourceIT
import me.devnatan.dockerkt.resource.exec.create
import me.devnatan.dockerkt.sleepForever
import me.devnatan.dockerkt.withContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CopyContainerArchivesIT : ResourceIT() {
    private val testImage = "alpine:latest"

    @Test
    fun `copy file from container`() =
        runTest {
            testClient.withContainer(
                testImage,
                {
                    command = listOf("sh", "-c", "echo 'test content' > /tmp/test.txt && sleep infinity")
                },
            ) { id ->
                testClient.containers.start(id)

                // Wait for file to be created
                delay(500)

                val tempDir = FileSystemUtils.createTempDirectory()
                try {
                    testClient.containers.copyFileFrom(
                        id,
                        "/tmp/test.txt",
                        tempDir.toString(),
                    )

                    val copiedFile = Path(tempDir, "test.txt")
                    assertTrue(FileSystemUtils.exists(copiedFile))

                    val content = FileSystemUtils.readFile(copiedFile).decodeToString()
                    assertEquals(
                        expected = "test content\n",
                        actual = content,
                    )
                } finally {
                    FileSystemUtils.deleteRecursively(tempDir)
                    testClient.containers.stop(id)
                }
            }
        }

    @Test
    fun `copy file to container`() =
        runTest {
            testClient.withContainer(
                testImage,
                {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val tempFile = FileSystemUtils.createTempFile()
                try {
                    FileSystemUtils.writeFile(tempFile, "hello from host".encodeToByteArray())

                    testClient.containers.copyFileTo(
                        id,
                        tempFile.toString(),
                        "/tmp/",
                    )

                    val execId =
                        testClient.exec.create(id) {
                            command = listOf("cat", "/tmp/${tempFile.name}")
                            attachStdout = true
                        }

                    val result = testClient.exec.start(execId, ExecStartOptions())
                    assertTrue(result is ExecStartResult.Complete)
                    assertTrue(result.output.contains("hello from host"))
                } finally {
                    FileSystemUtils.delete(tempFile)
                    testClient.containers.stop(id)
                }
            }
        }

    @Test
    fun `copy directory from container`() =
        runTest {
            testClient.withContainer(
                testImage,
                {
                    command =
                        listOf(
                            "sh",
                            "-c",
                            "mkdir -p /tmp/testdir && echo 'file1' > /tmp/testdir/file1.txt && echo 'file2' > /tmp/testdir/file2.txt && sleep infinity",
                        )
                },
            ) { id ->
                testClient.containers.start(id)

                delay(500)

                val tempDir = FileSystemUtils.createTempDirectory()
                try {
                    testClient.containers.copyDirectoryFrom(
                        id,
                        "/tmp/testdir",
                        tempDir.toString(),
                    )

                    val file1 = Path(tempDir, "testdir/file1.txt")
                    val file2 = Path(tempDir, "testdir/file2.txt")

                    assertTrue(FileSystemUtils.exists(file1))
                    assertTrue(FileSystemUtils.exists(file2))

                    assertEquals(
                        expected = "file1\n",
                        actual = FileSystemUtils.readFile(file1).decodeToString(),
                    )
                    assertEquals(
                        expected = "file2\n",
                        actual = FileSystemUtils.readFile(file2).decodeToString(),
                    )
                } finally {
                    FileSystemUtils.deleteRecursively(tempDir)
                    testClient.containers.stop(id)
                }
            }
        }

    @Test
    fun `copy directory to container`() =
        runTest {
            testClient.withContainer(
                testImage,
                {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                val tempDir = FileSystemUtils.createTempDirectory()
                try {
                    // Create test files
                    val file1 = Path(tempDir, "file1.txt")
                    val file2 = Path(tempDir, "file2.txt")

                    FileSystemUtils.writeFile(file1, "content1".encodeToByteArray())
                    FileSystemUtils.writeFile(file2, "content2".encodeToByteArray())

                    testClient.containers.copyDirectoryTo(
                        id,
                        tempDir.toString(),
                        "/tmp/",
                    )

                    val execId =
                        testClient.exec.create(id) {
                            command = listOf("sh", "-c", "cat /tmp/file1.txt && cat /tmp/file2.txt")
                            attachStdout = true
                        }

                    val result = testClient.exec.start(execId, ExecStartOptions())
                    assertTrue(result is ExecStartResult.Complete)

                    val output = result.output
                    assertTrue(
                        actual = output.contains("content1"),
                        message = "Expected 'content1' in output, but got: $output",
                    )
                    assertTrue(
                        actual = output.contains("content2"),
                        message = "Expected 'content2' in output, but got: $output",
                    )
                } finally {
                    FileSystemUtils.deleteRecursively(tempDir)
                    testClient.containers.stop(id)
                }
            }
        }

    @Test
    fun `copy fails when container not found`() =
        runTest {
            assertFailsWith<ContainerNotFoundException> {
                testClient.containers.copyFrom(
                    "nonexistent_container",
                    "/tmp/test.txt",
                )
            }
        }

    @Test
    fun `copy fails when path not found in container`() =
        runTest {
            testClient.withContainer(
                testImage,
                {
                    sleepForever()
                },
            ) { id ->
                testClient.containers.start(id)

                assertFailsWith<ArchiveNotFoundException> {
                    testClient.containers.copyFrom(
                        id,
                        "/nonexistent/path.txt",
                    )
                }

                testClient.containers.stop(id)
            }
        }
}
