package me.devnatan.dockerkt

/**
 * Creates a new Docker client instance for testing.
 * @param block The client configuration factory.
 */
fun createTestDockerClient(block: DockerClientConfigBuilder.() -> Unit = {}): DockerClient =
    runCatching {
        DockerClient {
            apply(block)
        }
    }.onFailure {
        @Suppress("TooGenericExceptionThrown")
        throw RuntimeException("Failed to initialize Docker test client", it)
    }.getOrThrow()
