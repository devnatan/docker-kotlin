package me.devnatan.dockerkt.models.volume

import kotlinx.serialization.Serializable

/**
 * Volume prune options.
 *
 * @property label Matches volumes that use a label.
 */
@Serializable
public data class VolumePruneOptions(
    public var label: String? = null,
)
