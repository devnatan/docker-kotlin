package me.devnatan.dockerkt.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * System information.
 *
 * @property id Unique identifier of the Docker daemon.
 * @property containers Total number of containers on the host.
 * @property containersRunning Number of containers with status "running".
 * @property containersPaused Number of containers with status "paused".
 * @property containersStopped Number of containers with status "stopped".
 * @property images Total number of images on the host.
 * @property driver Name of the storage driver in use.
 * @property driverStatus Key-value pairs with additional information about the storage driver.
 * @property dockerRootDir Root directory of persistent Docker state.
 * @property plugins Available plugins per type.
 * @property memoryLimit Indicates if memory limit support is enabled.
 * @property swapLimit Indicates if swap limit support is enabled.
 * @property cpuCfsPeriod Indicates if CPU CFS(Completely Fair Scheduler) period support is enabled.
 * @property cpuCfsQuota Indicates if CPU CFS(Completely Fair Scheduler) quota support is enabled.
 * @property cpuShares Indicates if CPU Shares limiting is supported by the host.
 * @property cpuSet Indicates if CPUsets (cpuset.cpus, cpuset.mems) are supported by the host.
 * @property pidsLimit Indicates if the host kernel has PIDs limit support enabled.
 * @property oomKillDisable Indicates if OOM Killer disabling is supported by the host.
 * @property ipv4Forwarding Indicates if IPv4 forwarding is enabled.
 * @property debug Indicates if the daemon is running in debug mode or has debug-level logging enabled.
 * @property nFd Total number of file descriptors that are in use by the daemon process. Only returned when debug mode is enabled.
 * @property nGoroutines Number of goroutines that are currently running in the daemon process. Only returned when debug mode is enabled.
 * @property systemTimeRaw Current system time of the host in RFC3339 format with nanosecond precision.
 * @property loggingDriver Default logging driver for new containers.
 * @property cgroupDriver Name of the cgroup driver in use.
 * @property cgroupVersion Version of cgroup used by host.
 * @property nEventsListener Number of event listeners that are currently subscribed to events.
 * @property kernelVersion The kernel version (`uname -r`) that the daemon is running on.
 * @property operatingSystem Name of the operating system that the daemon is running on.
 * @property osVersion Version of the operating system that the daemon is running on.
 * @property osType Operating system type. Can be `"linux"` or `"windows"`.
 * @property architecture The architecture that the daemon is running on. The value is in [GOARCH](https://golang.org/doc/install/source#environment) format.
 * @property nCPU Number of logical CPUs available to the daemon.
 * @property memTotal Total amount of memory available to the daemon in bytes.
 * @property indexServerAddress Address of the default docker registry.
 * @property registryConfig Configuration of the docker registries.
 * @property genericResources List of user-defined resources available on the node. Can be either integer or named string resources.
 * @property httpProxy HTTP proxy configured for the daemon. The value is taken from the environment variable `HTTP_PROXY`.
 * @property httpsProxy HTTPS proxy configured for the daemon. The value is taken from the environment variable `HTTPS_PROXY`.
 * @property noProxy Comma-separated list of hosts or subnets for which no proxy should be used. The value is taken from the environment variable `NO_PROXY`. Containers do not inherit this value.
 * @property name Hostname of the host running the daemon.
 * @property labels User-defined labels (key/value metadata) assigned to the daemon.
 * @property experimentalBuild Indicated if experimental features are enabled on the daemon.
 * @property serverVersion Current Docker daemon version as a string.
 * @property runtimes Available OCI runtimes on the host.
 * @property defaultRuntime Default OCI runtime that is used when starting containers.
 * @property swarm Configuration and status of Swarm mode on the daemon.
 * @property liveRestoreEnabled Indicates if live restore of containers is enabled.
 * @property isolation Default isolation technology used for containers.
 * @property initBinary Name of the binary used as init inside containers.
 * @property containerdCommit Containerd version information in SHA1 format.
 * @property runcCommit runc version information in SHA1 format.
 * @property initCommit Docker init version information in SHA1 format.
 * @property securityOptions List of security features that are enabled in the daemon.
 * @property productLicense Docker Product License summary.
 * @property defaultAddressPools List of custom default address pools for local networks.
 * @property firewallBackend Information about the firewall configuration used by the daemon. Currently only available on Linux.
 * @property discoveredDevices List of devices discovered by device drivers.
 * @property warnings List of warnings or informational messages about missing features or misconfigurations.
 * @property cdiSpecDirs List of directories where CDI(Container Device Interface) specs are located.
 * @property containerd Containerd-specific information.
 */
@Serializable
public data class SystemInfo internal constructor(
    @SerialName("ID") val id: String,
    @SerialName("Containers") val containers: Int,
    @SerialName("ContainersRunning") val containersRunning: Int,
    @SerialName("ContainersPaused") val containersPaused: Int,
    @SerialName("ContainersStopped") val containersStopped: Int,
    @SerialName("Images") val images: Int,
    @SerialName("Driver") val driver: String,
    @SerialName("DriverStatus") val driverStatus: List<List<String>>? = null,
    @SerialName("DockerRootDir") val dockerRootDir: String,
    @SerialName("Plugins") val plugins: Plugin,
    @SerialName("MemoryLimit") val memoryLimit: Boolean,
    @SerialName("SwapLimit") val swapLimit: Boolean,
    @SerialName("CpuCfsPeriod") val cpuCfsPeriod: Boolean,
    @SerialName("CpuCfsQuota") val cpuCfsQuota: Boolean,
    @SerialName("CPUShares") val cpuShares: Boolean,
    @SerialName("CPUSet") val cpuSet: Boolean,
    @SerialName("PidsLimit") val pidsLimit: Boolean,
    @SerialName("OomKillDisable") val oomKillDisable: Boolean,
    @SerialName("IPv4Forwarding") val ipv4Forwarding: Boolean,
    @SerialName("Debug") val debug: Boolean,
    @SerialName("NFd") val nFd: Int,
    @SerialName("NGoroutines") val nGoroutines: Int,
    @SerialName("SystemTime") val systemTimeRaw: String,
    @SerialName("LoggingDriver") val loggingDriver: String,
    @SerialName("CgroupDriver") val cgroupDriver: CgroupDriver,
    @SerialName("CgroupVersion") val cgroupVersion: String,
    @SerialName("NEventsListener") val nEventsListener: Int,
    @SerialName("KernelVersion") val kernelVersion: String,
    @SerialName("OperatingSystem") val operatingSystem: String,
    @SerialName("OSVersion") val osVersion: String,
    @SerialName("OSType") val osType: OSType,
    @SerialName("Architecture") val architecture: String,
    @SerialName("NCPU") val nCPU: Int,
    @SerialName("MemTotal") val memTotal: Long,
    @SerialName("IndexServerAddress") val indexServerAddress: String,
    @SerialName("RegistryConfig") val registryConfig: RegistryConfig,
    @SerialName("GenericResources") val genericResources: List<GenericResource>? = null,
    @SerialName("HttpProxy") val httpProxy: String? = null,
    @SerialName("HttpsProxy") val httpsProxy: String? = null,
    @SerialName("NoProxy") val noProxy: String? = null,
    @SerialName("Name") val name: String,
    @SerialName("Labels") val labels: List<String>? = null,
    @SerialName("ExperimentalBuild") val experimentalBuild: Boolean,
    @SerialName("ServerVersion") val serverVersion: String,
    @SerialName("Runtimes") val runtimes: Map<String, Runtime>,
    @SerialName("DefaultRuntime") val defaultRuntime: String,
    @SerialName("Swarm") val swarm: Swarm,
    @SerialName("LiveRestoreEnabled") val liveRestoreEnabled: Boolean,
    @SerialName("Isolation") val isolation: Isolation,
    @SerialName("InitBinary") val initBinary: String,
    @SerialName("ContainerdCommit") val containerdCommit: GitCommitInfo,
    @SerialName("RuncCommit") val runcCommit: GitCommitInfo,
    @SerialName("InitCommit") val initCommit: GitCommitInfo,
    @SerialName("SecurityOptions") val securityOptions: List<String>? = null,
    @SerialName("ProductLicense") val productLicense: String? = null,
    @SerialName("DefaultAddressPools") val defaultAddressPools: List<DefaultAddressPool>? = null,
    @SerialName("FirewallBackend") val firewallBackend: FirewallBackend? = null,
    @SerialName("DiscoveredDevices") val discoveredDevices: List<DiscoveredDevice>? = null,
    @SerialName("Warnings") val warnings: List<String>? = null,
    @SerialName("CDISpecDirs") val cdiSpecDirs: List<String>? = null,
    @SerialName("Containerd") val containerd: ContainerdInfo? = null,
) {
    /**
     * Plugins available per type.
     *
     * @property volume Names of available volume drivers and plugins.
     * @property network Names of available network drivers and plugins.
     * @property authorization Names of available authorization plugins.
     * @property log  Names of available logging drivers and plugins.
     */
    @Serializable
    public data class Plugin internal constructor(
        @SerialName("Volume") val volume: List<String>? = null,
        @SerialName("Network") val network: List<String>? = null,
        @SerialName("Authorization") val authorization: List<String>? = null,
        @SerialName("Log") val log: List<String>? = null,
    )

    /**
     * The driver used to manage cgroups on the host.
     */
    @Serializable
    public enum class CgroupDriver {
        @SerialName("cgroupfs")
        Cgroupfs,

        @SerialName("systemd")
        Systemd,

        @SerialName("none")
        None,

        Unknown,
    }

    /**
     * Operating system type.
     *
     * Currently, this can be either `"linux"` or `"windows"`.
     * Values other than these are considered as `Unknown`.
     *
     */
    @Serializable
    public enum class OSType {
        @SerialName("linux")
        Linux,

        @SerialName("windows")
        Windows,

        Unknown,
    }

    /**
     * Docker registry configuration.
     *
     * @property insecureRegistryCIDRs List of IP ranges of insecure registries, using the CIDR syntax (RFC 4632).
     * @property indexConfigs Configuration of registry indexes.
     * @property mirrors List of registry URLs that act as mirrors for official Docker registry (docker.io).
     */
    @Serializable
    public data class RegistryConfig internal constructor(
        @SerialName("InsecureRegistryCIDRs") val insecureRegistryCIDRs: List<String>? = null,
        @SerialName("IndexConfigs") val indexConfigs: Map<String, IndexConfig>,
        @SerialName("Mirrors") val mirrors: List<String>? = null,
    ) {
        /**
         * Configuration of a registry index.
         *
         * @property name Name of the registry, such as "docker.io".
         * @property mirrors List of mirrors of the registry, in URL format.
         * @property secure Indicates if the registry is part of the list of insecure registries. If `false`, the registry is considered insecure.
         * @property official Indicates if the registry is the official Docker registry (docker.io).
         */
        @Serializable
        public data class IndexConfig internal constructor(
            @SerialName("Name") val name: String,
            @SerialName("Mirrors") val mirrors: List<String>? = null,
            @SerialName("Secure") val secure: Boolean,
            @SerialName("Official") val official: Boolean,
        )
    }

    /**
     * User-defined resource available on the node.
     * Can be either integer or named string resources.
     *
     * @property namedResourceSpec Specification of a named string resource.
     * @property discreteResourceSpec Specification of a discrete integer resource.
     */
    @Serializable
    public data class GenericResource internal constructor(
        @SerialName("NamedResourceSpec") val namedResourceSpec: NamedResourceSpec? = null,
        @SerialName("DiscreteResourceSpec") val discreteResourceSpec: DiscreteResourceSpec? = null,
    ) {
        /**
         * Specification of a named string resource.
         *
         * @property kind Kind of the resource.
         * @property value Value of the resource.
         */
        @Serializable
        public data class NamedResourceSpec internal constructor(
            @SerialName("Kind") val kind: String,
            @SerialName("Value") val value: String,
        )

        /**
         * Specification of a discrete integer resource.
         *
         * @property kind Kind of the resource.
         * @property value Value of the resource.
         */
        @Serializable
        public data class DiscreteResourceSpec internal constructor(
            @SerialName("Kind") val kind: String,
            @SerialName("Value") val value: Long,
        )
    }

    /**
     * Specification of an OCI compliant runtime.
     *
     * @property path Name and optional path of the OCI executable binary.
     * @property runtimeArgs List of command-line arguments to pass to the runtime when invoked.
     * @property status Key-value pairs with additional information about the runtime.
     */
    @Serializable
    public data class Runtime internal constructor(
        @SerialName("path") val path: String,
        @SerialName("runtimeArgs") val runtimeArgs: List<String>? = null,
        @SerialName("status") val status: Map<String, String>? = null,
    )

    /**
     * Configuration and status of Swarm mode on the daemon.
     *
     * @property nodeID Unique identifier of the node in the swarm.
     * @property nodeAddr IP address and port of the node that can be reached by other nodes in the swarm.
     * @property localNodeState Current local status of this node.
     * @property controlAvailable Indicates if this node is a swarm manager.
     * @property error Error message, if the node is in an error state.
     * @property remoteManagers List of IDs and addresses of other managers in the swarm.
     * @property nodes Total number of nodes in the swarm.
     * @property managers Total number of managers in the swarm.
     * @property cluster Detailed information about the swarm cluster.
     */
    @Serializable
    public data class Swarm internal constructor(
        @SerialName("NodeID") val nodeID: String,
        @SerialName("NodeAddr") val nodeAddr: String,
        @SerialName("LocalNodeState") val localNodeState: LocalNodeState,
        @SerialName("ControlAvailable") val controlAvailable: Boolean,
        @SerialName("Error") val error: String,
        @SerialName("RemoteManagers") val remoteManagers: List<RemoteManager>? = null,
        @SerialName("Nodes") val nodes: Int? = null,
        @SerialName("Managers") val managers: Int? = null,
        @SerialName("Cluster") val cluster: ClusterInfo? = null,
    ) {
        /**
         * Current local status of this node.
         */
        @Serializable
        public enum class LocalNodeState {
            @SerialName("inactive")
            Inactive,

            @SerialName("pending")
            Pending,

            @SerialName("active")
            Active,

            @SerialName("error")
            Error,

            @SerialName("locked")
            Locked,

            Unknown,
        }

        /**
         * ID and address of another manager in the swarm.
         */
        @Serializable
        public data class RemoteManager internal constructor(
            @SerialName("NodeID") val nodeID: String,
            @SerialName("Addr") val addr: String,
        )

        /**
         * Detailed information about the swarm cluster.
         *
         * @property id Unique identifier of the swarm cluster.
         * @property version The version number of the swarm cluster object.
         * @property createdAtRaw The date and time when the swarm cluster was created.
         * @property updatedAtRaw The date and time when the swarm cluster was last updated.
         * @property spec User modifiable swarm configuration.
         * @property tlsInfo Information about the issuer of leaf TLS certificates and trusted root CA certificate.
         * @property rootRotationInProgress Indicates if a root CA rotation is currently in progress for the swarm.
         * @property dataPathPort The port number used for data path traffic (overlay network and swarm services).
         * @property defaultAddrPool List of default subnet pools for global scope networks.
         * @property subnetSize Subnet size of the networks created from the default subnet pool.
         */
        @Serializable
        public data class ClusterInfo internal constructor(
            @SerialName("ID") val id: String,
            @SerialName("Version") val version: ClusterVersion,
            @SerialName("CreatedAt") val createdAtRaw: String,
            @SerialName("UpdatedAt") val updatedAtRaw: String,
            @SerialName("Spec") val spec: ClusterSpec,
            @SerialName("TLSInfo") val tlsInfo: TLSInfo,
            @SerialName("RootRotationInProgress") val rootRotationInProgress: Boolean,
            @SerialName("DataPathPort") val dataPathPort: Int,
            @SerialName("DefaultAddrPool") val defaultAddrPool: List<String>? = null,
            @SerialName("SubnetSize") val subnetSize: Int,
        ) {
            /**
             * The version number of the swarm cluster object.
             *
             * @property index Index of the swarm cluster object such as node, service, etc.
             */
            @Serializable
            public data class ClusterVersion internal constructor(
                @SerialName("Index") val index: Long,
            )

            /**
             * User modifiable swarm configuration.
             *
             * @property name Name of the swarm cluster.
             * @property labels User-defined labels (key/value metadata) assigned to the swarm cluster.
             * @property orchestration Orchestration configuration for the swarm cluster.
             * @property raft Raft configuration for the swarm cluster.
             * @property dispatcher Dispatcher configuration for the swarm cluster.
             * @property caConfig Certificate Authority (CA) configuration for the swarm cluster.
             * @property encryptionConfig Parameters related to encryption at rest.
             * @property taskDefaults Default parameters for creating tasks in this cluster.
             */
            @Serializable
            public data class ClusterSpec internal constructor(
                @SerialName("Name") val name: String,
                @SerialName("Labels") val labels: Map<String, String>,
                @SerialName("Orchestration") val orchestration: Orquestration? = null,
                @SerialName("Raft") val raft: Raft,
                @SerialName("Dispatcher") val dispatcher: Dispatcher? = null,
                @SerialName("CAConfig") val caConfig: CAConfig? = null,
                @SerialName("EncryptionConfig") val encryptionConfig: EncryptionConfig,
                @SerialName("TaskDefaults") val taskDefaults: TaskDefaults,
            ) {
                /**
                 * Orchestration configuration for the swarm cluster.
                 *
                 * @property taskHistoryRetentionLimit The number of historical tasks to keep per instance or node.
                 *                                     If negative, never remove completed or failed tasks.
                 *                                     If null, defaults to the swarm default.
                 */
                @Serializable
                public data class Orquestration internal constructor(
                    @SerialName("TaskHistoryRetentionLimit") val taskHistoryRetentionLimit: Long? = null,
                )

                /**
                 * Raft configuration for the swarm cluster.
                 *
                 * @property snapshotInterval Number of log entries between snapshots.
                 * @property keepOldSnapshots Number of snapshots to keep beyond the current snapshot.
                 * @property logEntriesForSlowFollowers Number of log entries to keep around to sync up slow followers after a snapshot is created.
                 * @property electionTick Number of ticks that a follower waits for a message from leader before becoming a candidate and starting an election. Must be greater than heartbeat tick.
                 * @property heartbeatTick Number of ticks between heartbeats. Every HeartbeatTick, the leader sends a heartbeat to all followers. Must be less than election tick.
                 */
                @Serializable
                public data class Raft internal constructor(
                    @SerialName("SnapshotInterval") val snapshotInterval: Long,
                    @SerialName("KeepOldSnapshots") val keepOldSnapshots: Long,
                    @SerialName("LogEntriesForSlowFollowers") val logEntriesForSlowFollowers: Long,
                    @SerialName("ElectionTick") val electionTick: Int,
                    @SerialName("HeartbeatTick") val heartbeatTick: Int,
                )

                /**
                 * Dispatcher configuration for the swarm cluster.
                 *
                 * @property heartbeatPeriod The delay for an agent to send a heartbeat to the dispatcher.
                 */
                @Serializable
                public data class Dispatcher internal constructor(
                    @SerialName("HeartbeatPeriod") val heartbeatPeriod: Long,
                )

                /**
                 * Certificate Authority (CA) configuration for the swarm cluster.
                 *
                 * @property nodeCertExpiry The duration node certificates are issued for, in nanoseconds.
                 * @property externalCAs Configuration for forwarding signing requests to an external CA.
                 * @property signingCACert The designed CA certificate for all swarm node TLS leaf certificates, in PEM format.
                 * @property signingCAKey The private key of the designed CA, in PEM format.
                 * @property forceRotate A counter that can be incremented to force swarm to generate new CA certificate and key.
                 */
                @Serializable
                public data class CAConfig internal constructor(
                    @SerialName("NodeCertExpiry") val nodeCertExpiry: Long,
                    @SerialName("ExternalCAs") val externalCAs: List<ExternalCA>? = null,
                    @SerialName("SigningCACert") val signingCACert: String,
                    @SerialName("SigningCAKey") val signingCAKey: String,
                    @SerialName("ForceRotate") val forceRotate: Long,
                ) {
                    /**
                     * Configuration for forwarding signing requests to an external CA.
                     *
                     * @property protocol The protocol used to communicate with the external CA. Currently only "cfssl" is supported.
                     * @property url URL where certificate signing requests should be sent.
                     * @property options Key-value pairs with additional options for the external CA driver.
                     * @property caCert The root CA certificate this external CA uses to issue TLS certificates, in PEM format.
                     */
                    @Serializable
                    public data class ExternalCA internal constructor(
                        @SerialName("Protocol") val protocol: String,
                        @SerialName("URL") val url: String,
                        @SerialName("Options") val options: Map<String, String>,
                        @SerialName("CACert") val caCert: String,
                    )
                }

                /**
                 * Parameters related to encryption at rest.
                 *
                 * @property autoLockManagers Indicates if set, generate a key and use it to lock data stored on the managers.
                 */
                @Serializable
                public data class EncryptionConfig internal constructor(
                    @SerialName("AutoLockManagers") val autoLockManagers: Boolean,
                )

                /**
                 * Default parameters for creating tasks in this cluster.
                 *
                 * @property logDriver The log driver to use for new tasks created in the orchestrator if unspecified by a service.
                 */
                @Serializable
                public data class TaskDefaults internal constructor(
                    @SerialName("LogDriver") val logDriver: LogDriver,
                ) {
                    /**
                     * The log driver to use for new tasks created in the orchestrator if unspecified by a service.
                     *
                     * @property name Name of the log driver.
                     * @property options Key-value pairs with additional options for the log driver.
                     */
                    @Serializable
                    public data class LogDriver internal constructor(
                        @SerialName("Name") val name: String,
                        @SerialName("Options") val options: Map<String, String>,
                    )
                }
            }

            /**
             * Information about the issuer of leaf TLS certificates and trusted root CA certificate.
             *
             * @property trustRoot The root CA certificate(s) that are used to validate TLS leaf certificates, in PEM format.
             * @property certIssuerSubject The base64-url-safe-encoded raw subject bytes of the issuer.
             * @property certIssuerPublicKey The base64-url-safe-encoded raw public key bytes of the issuer.
             */
            @Serializable
            public data class TLSInfo internal constructor(
                @SerialName("TrustRoot") val trustRoot: String,
                @SerialName("CertIssuerSubject") val certIssuerSubject: String,
                @SerialName("CertIssuerPublicKey") val certIssuerPublicKey: String,
            )
        }
    }

    /**
     * Default isolation technology used for containers.
     *
     * Values other than "default", "hyperv", or "process" are considered as `Unknown`.
     */
    @Serializable
    public enum class Isolation {
        @SerialName("default")
        Default,

        @SerialName("hyperv")
        Hyperv,

        @SerialName("process")
        Process,

        @SerialName("")
        Unknown,
    }

    /**
     * Git commit information.
     *
     * @property id SHA1 identifier of the commit.
     */
    @Serializable
    public data class GitCommitInfo internal constructor(
        @SerialName("ID") val id: String,
    )

    /**
     * Custom default address pool for local networks.
     *
     * @property base The network address in CIDR format
     * @property size The network pool size
     */
    @Serializable
    public data class DefaultAddressPool internal constructor(
        @SerialName("Base") val base: String,
        @SerialName("Size") val size: Int,
    )

    /**
     * Information about the firewall configuration used by the daemon.
     * Currently only available on Linux.
     *
     * @property driver Name of the firewall backend driver.
     * @property info List of key-value pairs with additional information about the firewall backend.
     */
    @Serializable
    public data class FirewallBackend internal constructor(
        @SerialName("Driver") val driver: String,
        @SerialName("Info") val info: List<String>? = null,
    )

    /**
     * Device discovered by device drivers.
     *
     * @property source The origin device driver
     * @property id The unique identifier for the device within its source driver.
     *              For CDI devices, this would be an FQDN like "vendor.com/gpu=0".
     */
    @Serializable
    public data class DiscoveredDevice internal constructor(
        @SerialName("Source") val source: String,
        @SerialName("ID") val id: String,
    )

    /**
     * Information for connecting to the containerd instance that is used by the daemon.
     * This is included for debugging purposes only.
     *
     * @property address The address of the containerd socket.
     * @property namespace Namespaces used by the daemon for running containers and plugins in containerd.
     */
    @Serializable
    public data class ContainerdInfo internal constructor(
        @SerialName("Address") val address: String,
        @SerialName("Namespaces") val namespace: Namespaces,
    ) {
        /**
         * Namespaces used by the daemon for running containers and plugins in containerd.
         *
         * @property containers The default containerd namespace used for containers managed by the daemon. Default is "moby".
         * @property plugins The default containerd namespace used for plugins managed by the daemon.
         */
        @Serializable
        public data class Namespaces internal constructor(
            @SerialName("Containers") val containers: String,
            @SerialName("Plugins") val plugins: String,
        )
    }
}
