package me.devnatan.dockerkt.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class NetworkContainer(
    @SerialName("Name") val name: String,
    @SerialName("EndpointID") val endpointId: String,
    @SerialName("MacAddress") val macAddress: String,
    @SerialName("IPv4Address") val ipv4Address: Boolean,
    @SerialName("IPv6Address") val ipv6Address: Boolean,
)
