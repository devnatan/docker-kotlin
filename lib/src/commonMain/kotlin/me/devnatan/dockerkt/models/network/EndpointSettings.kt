package me.devnatan.dockerkt.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class EndpointSettings(
    @SerialName("IPAMConfig") var ipamConfig: EndpointIPAMConfig? = null,
    @SerialName("Links") var links: List<String>? = null,
    @SerialName("Aliases") var aliases: List<String>? = null,
    @SerialName("NetworkID") var networkID: String? = null,
    @SerialName("EndpointID") var endpointID: String? = null,
    @SerialName("Gateway") var gateway: String? = null,
    @SerialName("IPAddress") var ipAddress: String? = null,
    @SerialName("IPPrefixLen") var ipPrefixLen: Int? = null,
    @SerialName("IPv6Gateway") var ipv6Gateway: String? = null,
    @SerialName("GlobalIPv6Gateway") var globalIpv6Gateway: String? = null,
    @SerialName("GlobalIPv6PrefixLen") var globalIpv6PrefixLen: Long? = null,
    @SerialName("MacAddress") var macAddress: String? = null,
    @SerialName("DriverOpts") var driverOpts: Map<String, String>? = null,
)

@Serializable
public data class EndpointIPAMConfig(
    @SerialName("IPv4Address") var ipv4Address: String? = null,
    @SerialName("IPv6Address") var ipv6Address: String? = null,
    @SerialName("LinkLocalIPs") var linkLocalIPs: List<String>? = null,
)
