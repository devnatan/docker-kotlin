package me.devnatan.dockerkt.models.network

import kotlinx.serialization.Serializable

/**
 * Network inspect options
 *
 * @property verbose Detailed inspect output for troubleshooting (default: false).
 * @property scope Filter the network by scope (swarm, global, or local).
 * @see NetworkLocalScope
 * @see NetworkGlobalScope
 * @see NetworkSwarmScope
 * @see NetworkResource.inspect
 */
@Serializable
public data class NetworkInspectOptions(
    public var verbose: Boolean? = null,
    public var scope: String? = null,
) {
    init {
        scope?.let { requireNetworkScope(it) }
    }
}

private fun requireNetworkScope(scope: String) =
    require(scope == NetworkLocalScope || scope == NetworkGlobalScope || scope == NetworkSwarmScope) {
        "Network scope must be $NetworkLocalScope, $NetworkGlobalScope or $NetworkSwarmScope"
    }
