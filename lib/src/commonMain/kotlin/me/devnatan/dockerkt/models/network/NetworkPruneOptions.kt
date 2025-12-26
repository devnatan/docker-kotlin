package me.devnatan.dockerkt.models.network

import kotlinx.serialization.Serializable

/**
 * Network prune options.
 *
 * @property until Prune networks created before this timestamp.
 * The timestamp can be Unix timestamps, date formatted timestamps,
 * or Go duration strings (e.g. `10m`, `1h30m`) computed relative to the daemon machine’s time.
 * @property label Prune networks with (or without, in case `label!=...` is used) the specified labels.
 */
@Serializable
public data class NetworkPruneOptions(
    public var until: String? = null,
    public var label: String? = null,
)
