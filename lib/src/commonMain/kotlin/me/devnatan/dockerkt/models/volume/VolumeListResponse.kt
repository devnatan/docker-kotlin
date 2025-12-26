package me.devnatan.dockerkt.models.volume

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Volume list response.
 *
 * @property volumes List of volumes.
 * @property warnings Warnings that occurred when fetching the list of volumes.
 */
@Serializable
public data class VolumeListResponse(
    @SerialName("Volumes") public val volumes: List<Volume>,
    @SerialName("Warnings") public val warnings: List<String>?,
)
