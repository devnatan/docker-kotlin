package me.devnatan.dockerkt.models.exec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.devnatan.dockerkt.models.ProcessConfig

@Serializable
public data class ExecInspectResponse internal constructor(
    @SerialName("CanRemove") val canRemove: Boolean,
    @SerialName("DetachKeys") val detachKeys: String,
    @SerialName("ID") val id: String,
    @SerialName("Running") val running: Boolean,
    @SerialName("ExitCode") val exitCode: Int,
    @SerialName("ProcessConfig") val processConfig: ProcessConfig,
    @SerialName("OpenStdin") val openStdin: Boolean,
    @SerialName("OpenStderr") val openStdErr: Boolean,
    @SerialName("OpenStdout") val openStdout: Boolean,
    @SerialName("ContainerID") val containerId: String,
    @SerialName("Pid") val pid: Int,
)
