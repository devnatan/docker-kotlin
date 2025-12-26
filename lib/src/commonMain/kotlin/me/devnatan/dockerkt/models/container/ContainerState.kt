@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.devnatan.dockerkt.models.Healthcheck
import kotlin.jvm.JvmStatic
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class ContainerState internal constructor(
    @SerialName("Status") public val statusString: String,
    @SerialName("Running") public val isRunning: Boolean,
    @SerialName("Paused") public val isPaused: Boolean,
    @SerialName("Restarting") public val isRestarting: Boolean,
    @SerialName("Dead") public val isDead: Boolean,
    @SerialName("StartedAt") public val startedAtRaw: String,
    @SerialName("FinishedAt") public val finishedAtRaw: String,
    @SerialName("Pid") public val pid: Int? = null,
    @SerialName("ExitCode") public val exitCode: Int? = null,
    @SerialName("Error") public val error: String? = null,
    @SerialName("OOMKilled") public val oomKilled: Boolean,
    @SerialName("Health") public val health: Healthcheck? = null,
) {
    public val status: Status = Status.parse(statusString)
    public val startedAt: Instant by lazy { Instant.parse(startedAtRaw) }
    public val finishedAt: Instant by lazy { Instant.parse(finishedAtRaw) }

    @Serializable
    public sealed class Status(
        public val value: String,
    ) {
        public object Created : Status("created")

        public object Running : Status("running")

        public object Paused : Status("paused")

        public object Restarting : Status("restarting")

        public object Removing : Status("removing")

        public object Exited : Status("exited")

        public object Dead : Status("dead")

        public class Other(
            value: String,
        ) : Status(value)

        public companion object {
            public val all: Set<Status> by lazy {
                setOf(Created, Running, Paused, Restarting, Removing, Exited, Dead)
            }

            @JvmStatic
            public fun parse(value: String): Status = all.firstOrNull { it.value == value } ?: Other(value)
        }
    }
}
