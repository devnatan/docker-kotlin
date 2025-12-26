package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.devnatan.dockerkt.models.ExposedPort
import me.devnatan.dockerkt.models.Mount
import me.devnatan.dockerkt.models.MountBindOptions
import me.devnatan.dockerkt.models.PortBinding
import me.devnatan.dockerkt.models.PortBindingsSerializer
import me.devnatan.dockerkt.models.network.EndpointSettings

@Serializable
public data class Container internal constructor(
    @SerialName("Id") val id: String,
    @SerialName("Created") val createdAt: String,
    @SerialName("Path") val path: String? = null,
    @SerialName("Args") val args: List<String> = emptyList(),
    @SerialName("State") val state: ContainerState,
    @SerialName("Image") val image: String,
    @SerialName("ResolvConfPath") val resolvConfPath: String? = null,
    @SerialName("HostnamePath") val hostnamePath: String? = null,
    @SerialName("HostsPath") val hostsPath: String? = null,
    @SerialName("LogPath") val logsPath: String? = null,
    @SerialName("Name") val name: String,
    @SerialName("RestartCount") val restartCount: Int,
    @SerialName("Driver") val driver: String,
    @SerialName("Platform") val platform: String,
    @SerialName("Mountlabel") val mountLabel: String? = null,
    @SerialName("ProcessLabel") val processLabel: String? = null,
    @SerialName("AppArmorProfile") val appArmorProfile: String? = null,
    @SerialName("ExecIDs") val execIds: List<String>? = null,
    @SerialName("SizeRw") val sizeRw: Long? = null,
    @SerialName("SizeRootFs") val sizeRootFs: Long? = null,
    @SerialName("NetworkSettings") val networkSettings: NetworkSettings,
    @SerialName("Config") public val config: ContainerConfig,
    @SerialName("Mounts") public val mounts: List<Mount>,
)

@Serializable
public data class NetworkSettings internal constructor(
    @SerialName("Bridge") public val bridge: String,
    @SerialName("SandboxID") public val sandboxId: String,
    @SerialName("HairpinMode") public val hairpinMode: Boolean,
    @SerialName("LinkLocalIPv6Address") public val linkLocalIPv6Address: String,
    @SerialName("LinkLocalIPv6PrefixLen") public val linkLocalIPv6PrefixLength: Int,
    @SerialName("GlobalIPv6Address") public val globalIPv6Address: String? = null,
    @SerialName("GlobalIPv6PrefixLen") public val globalIPv6PrefixLength: Int? = null,
    @SerialName("IPAddress") public val ipAddress: String? = null,
    @SerialName("IPPrefixLen") public val ipAddressPrefixLength: Int? = null,
    @SerialName("IPv6Gateway") public val ipv6Gateway: String? = null,
    @SerialName("MacAddress") public val macAddress: String? = null,
    @SerialName("Ports") public val ports:
        @Serializable(with = PortBindingsSerializer::class)
        Map<ExposedPort, List<PortBinding>?> =
        emptyMap(),
    @SerialName("SandboxKey") public val sandboxKey: String,
    @SerialName("EndpointID") public val endpointId: String,
    @SerialName("Gateway") public val gateway: String,
    @SerialName("Networks") public val networks: Map<String, EndpointSettings> = emptyMap(),
)

@Serializable
public data class MountPoint internal constructor(
    @SerialName("Name") public val name: String,
    @SerialName("Source") public val source: String,
    @SerialName("Destination") public val dest: String,
    @SerialName("Local") public val driver: String,
    @SerialName("Mode") public val mode: String,
    @SerialName("RW") public val rw: Boolean,
    @SerialName("Propagation") public val propagation: MountBindOptions.Propagation,
)
