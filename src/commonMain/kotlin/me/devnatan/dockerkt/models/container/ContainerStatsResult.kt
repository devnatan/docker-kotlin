package me.devnatan.dockerkt.models.container

import kotlinx.coroutines.flow.Flow

/**
 * Result of a [me.devnatan.dockerkt.resource.container.ContainerResource.stats] operation.
 */
public sealed class ContainerStatsResult {
    /**
     * Streaming result. The [output] flow emits one [ContainerStats] per Docker
     * stats message until the container stops or the flow is cancelled.
     */
    public data class Stream(
        val output: Flow<ContainerStats>,
    ) : ContainerStatsResult()

    /**
     * Single-snapshot result returned when `stream = false`.
     */
    public data class Single(
        val output: ContainerStats,
    ) : ContainerStatsResult()
}
