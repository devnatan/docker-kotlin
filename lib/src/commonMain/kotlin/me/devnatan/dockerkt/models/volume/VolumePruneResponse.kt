package me.devnatan.dockerkt.models.volume

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Volume prune response.
 *
 * @property volumesDeleted List of deleted volumes ids.
 * @property spaceReclaimed Disk space reclaimed.
 */
@Serializable
public data class VolumePruneResponse internal constructor(
    @SerialName("VolumesDeleted") public val volumesDeleted: List<String>?,
    @SerialName("SpaceReclaimed") public val spaceReclaimed: Long,
)
