package me.devnatan.dockerkt.models.volume

/**
 * Volume remove options.
 *
 * @property force When set to `true` volumes will be removed even if they are in use by containers.
 */
public data class VolumeRemoveOptions(
    var force: Boolean = false,
)
