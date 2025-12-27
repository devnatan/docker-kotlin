package me.devnatan.dockerkt.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network IPAM (IP Address Management) is the administration of DNS and DHCP, which are the network services that
 * assign and resolve IP addresses to containers in a TCP/IP network.
 *
 * @property driver Name of the IPAM driver to use.
 * @property config List of IPAM configuration options.
 * @property options Driver-specific options, specified as a user defined metadata.
 */
@Serializable
public data class IPAM(
    @SerialName("Driver") public val driver: String,
    @SerialName("Config") public val config: List<IPAMConfig>? = null,
    @SerialName("Options") public val options: Map<String, String?>? = null,
)

/**
 * IP Address management configuration for a network.
 *
 * TODO docs on properties
 */
@Serializable
public data class IPAMConfig(
    @SerialName("Subnet") public val subnet: String,
    @SerialName("IPRange") public val ipRange: String?,
    @SerialName("Gateway") public val gateway: String,
)
