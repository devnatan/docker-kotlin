package me.devnatan.dockerkt.models.container

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.devnatan.dockerkt.models.Frame

@Serializable
public data class ContainerAttachOptions(
    @Transient public var stdin: Boolean = false,
    @Transient public var stdout: Boolean = false,
    @Transient public var stderr: Boolean = false,
    @Transient public var stream: Boolean = false,
    @Transient public var logs: Boolean = false,
    @Transient public var detachKeys: String? = null,
)

public sealed class ContainerAttachResult {
    public object Detached : ContainerAttachResult()

    public data class Stream(
        public val output: Flow<Frame>,
        internal val input: suspend (ByteArray) -> Unit,
    ) : ContainerAttachResult() {
        public suspend fun write(data: ByteArray): Unit = input(data)

        public suspend fun write(data: String): Unit = write(data.encodeToByteArray())
    }
}
