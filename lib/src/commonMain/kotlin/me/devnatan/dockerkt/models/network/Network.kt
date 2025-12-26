package me.devnatan.dockerkt.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Network(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
    @SerialName("Scope") val scope: String = NetworkLocalScope,
    @SerialName("Driver") val driver: String = NetworkBridgeDriver,
    @SerialName("EnableIPV6") val enableIPv6: Boolean = false,
    @SerialName("Internal") val isInternal: Boolean = false,
    @SerialName("Attachable") val isAttachable: Boolean = false,
    @SerialName("Ingress") val ingress: Boolean = false,
    @SerialName("Containers") val containers: Map<String, NetworkContainer> = emptyMap(),
    @SerialName("Options") val options: Map<String, String> = emptyMap(),
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
)

public const val NetworkCustomType: String = "custom"
public const val NetworkBuiltinType: String = "builtin"

public const val NetworkLocalScope: String = "local"
public const val NetworkGlobalScope: String = "global"
public const val NetworkSwarmScope: String = "swarm"

/**
 * The default network driver.
 * Bridge networks are usually used when your applications run in standalone containers that need to communicate.
 *
 * Are best when you need multiple containers to communicate on the same Docker host.
 *
 * @see <a href="https://docs.docker.com/network/bridge/">Bridge networks</a>
 */
public const val NetworkBridgeDriver: String = "bridge"

/**
 * For standalone containers, remove network isolation between the container and the Docker host, and use the host’s networking directly.
 * Good when the network stack should not be isolated from the Docker host, but you want other aspects of the container to be isolated.
 *
 * @see <a href="https://docs.docker.com/network/host/">Host networks</a>
 */
public const val NetworkHostDriver: String = "host"

/**
 * Overlay networks connect multiple Docker daemons together and enable swarm services to communicate with each other.
 * You can also use overlay networks to facilitate communication between a swarm service and a standalone container,
 * or between two standalone containers on different Docker daemons.
 *
 * This strategy removes the need to do OS-level routing between these containers.
 *
 * Use it when you need containers running on different Docker hosts to communicate,
 * or when multiple applications work together using swarm services.
 *
 * @see <a href="https://docs.docker.com/network/overlay/">Overlay networks</a>
 */
public const val NetworkOverlayDriver: String = "overlay"

/**
 * IPvlan networks give users total control over both IPv4 and IPv6 addressing.
 *
 * The VLAN driver builds on top of that in giving operators complete control of layer 2 VLAN tagging
 * and even IPvlan L3 routing for users interested in underlay network integration.
 *
 * @see <a href="https://docs.docker.com/network/ipvlan/">IPvlan networks</a>
 */
public const val NetworkIPvlanDriver: String = "ipvlan"

/**
 * Macvlan networks allow you to assign a MAC address to a container, making it appear as a physical device on your network.
 * The Docker daemon routes traffic to containers by their MAC addresses.
 *
 * Using the macvlan driver is sometimes the best choice when dealing with legacy applications
 * that expect to be directly connected to the physical network, rather than routed through the Docker host’s network stack.
 *
 * @see <a href="https://docs.docker.com/network/macvlan/">Macvlan networks</a>
 */
public const val NetworkMacvlanDriver: String = "macvlan"

/**
 * Disables all network for the container.
 * Usually used in conjunction with a custom network driver. `none` is not available in Swarm mode.
 * @see <a href="https://docs.docker.com/network/none/">Disabling container networking</a>
 */
public const val NetworkNoneDriver: String = "none"
