package me.devnatan.dockerkt.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

public typealias NetworkDriver = String

/**
 * Network configuration.
 *
 * @property name The network's name.
 * @property checkDuplicate Check for networks with duplicate names.
 * Since Network is primarily keyed based on a random ID and not on the name,
 * and network name is strictly a user-friendly alias to the network which is uniquely identified using ID,
 * there is no guaranteed way to check for duplicates. CheckDuplicate is there to provide the best effort checking
 * of any networks which has the same name, but it is not guaranteed to catch all name collisions.
 * @property driver Name of the network driver plugin to use.
 * @property isInternal Restrict external access to the network.
 * @property isAttachable Globally scoped network is manually attachable by regular containers from workers in swarm
 * mode.
 * @property ingress Ingress network is the network which provides the routing-mesh in swarm mode.
 * @property ipam IPAM configuration.
 * @property enableIpv6 Enable IPv6 on the network.
 * @property options Network specific options to be used by the drivers.
 * @property labels User-defined key/value metadata.
 */
@Serializable
public data class NetworkCreateOptions
    @JvmOverloads
    public constructor(
        @SerialName("Name") public var name: String? = null,
        @SerialName("CheckDuplicate") public var checkDuplicate: Boolean? = null,
        @SerialName("Driver") public var driver: NetworkDriver? = null,
        @SerialName("Internal") public var isInternal: Boolean? = null,
        @SerialName("Attachable") public var isAttachable: Boolean? = null,
        @SerialName("Ingress") public var ingress: Boolean? = null,
        @SerialName("IPAM") public var ipam: IPAM? = null,
        @SerialName("EnableIPV6") public var enableIpv6: Boolean? = null,
        @SerialName("Options") public var options: Map<String, String?>? = null,
        @SerialName("Labels") public var labels: Map<String, String?>? = null,
    ) {
        public companion object {
            /**
             * Disable all networking. Usually used in conjunction with a custom network driver.
             * This network driver is not available for swarm services.
             */
            public const val NONE: NetworkDriver = "none"

            /**
             * The default network driver.
             */
            public const val BRIDGE: NetworkDriver = "bridge"

            /**
             * For standalone containers, remove network isolation between the container and the Docker host, and use the
             * host’s networking directly.
             */
            public const val HOST: NetworkDriver = "host"

            /**
             * Overlay networks connect multiple Docker daemons together and enable swarm services to communicate with each
             * other. You can also use overlay networks to facilitate communication between a swarm service and a standalone
             * container, or between two standalone containers on different Docker daemons.
             */
            public const val OVERLAY: NetworkDriver = "overlay"

            /**
             * IPvlan networks give users total control over both IPv4 and IPv6 addressing.
             *
             * The VLAN driver builds on top of that in giving operators complete control of layer 2 VLAN tagging and even
             * IPvlan L3 routing for users interested in underlay network integration.
             */
            public const val IPVLAN: NetworkDriver = "ipvlan"

            /**
             * Macvlan networks allow you to assign a MAC address to a container, making it appear as a physical device on
             * your network. The Docker daemon routes traffic to containers by their MAC addresses.
             *
             * Using the macvlan driver is sometimes the best choice when dealing with legacy applications that expect to be
             * directly connected to the physical network, rather than routed through the Docker host’s network stack.
             */
            public const val MACVLAN: NetworkDriver = "macvlan"
        }
    }
