@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class Healthcheck internal constructor(
    // TODO create sealed class for status
    @SerialName("Status") public val statusString: String,
    @SerialName("FailingStreak") public val failingStreak: Int,
    @SerialName("Log") public val logs: List<HealthcheckResult> = emptyList(),
)

@Serializable
public data class HealthcheckResult internal constructor(
    @SerialName("Start") public val startedAtString: String,
    @SerialName("End") public val endedAtString: String? = null,
    @SerialName("ExitCode") public val exitCode: Int? = null,
    @SerialName("Output") public val output: String? = null,
) {
    public val startedAt: Instant get() = Instant.parse(startedAtString)
    public val endedAt: Instant? get() = endedAtString?.let(Instant::parse)
}
