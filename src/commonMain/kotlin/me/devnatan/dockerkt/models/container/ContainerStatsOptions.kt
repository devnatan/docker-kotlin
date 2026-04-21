package me.devnatan.dockerkt.models.container

import kotlin.jvm.JvmOverloads

/**
 * Container stats endpoint options.
 *
 * @property stream When `true` (default), stats are pulled continuously as a stream.
 *                  When `false`, a single snapshot is returned.
 * @property oneShot Only applicable when [stream] is `false`. When `true`, the stats
 *                   are returned immediately without a 1-second pre-read that Docker
 *                   performs by default to compute CPU usage deltas.
 */
public class ContainerStatsOptions
    @JvmOverloads
    constructor(
        public var stream: Boolean = true,
        public var oneShot: Boolean = false,
    )
