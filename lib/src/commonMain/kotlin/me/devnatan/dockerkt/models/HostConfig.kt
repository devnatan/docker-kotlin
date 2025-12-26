package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
public data class HostConfig
    @JvmOverloads
    public constructor(
        @SerialName("CpuShares") public var cpuShares: Int? = null,
        @SerialName("Memory") public var memory: Long? = null,
        @SerialName("CgroupParent") public var cgroupParent: String? = null,
        @SerialName("BlkioWeight") public var blkioWeight: Int? = null,
        @SerialName("BlkioWeightDevice") public var blkioWeightDevice: List<BlkioWeightDevice>? = null,
        @SerialName("BlkioDeviceReadBps") public var blkioDeviceReadBps: List<ThrottleDevice>? = null,
        @SerialName("BlkioDeviceWriteBps") public var blkioDeviceWriteBps: List<ThrottleDevice>? = null,
        @SerialName("BlkioDeviceReadIOps") public var blkioDeviceReadIOps: List<ThrottleDevice>? = null,
        @SerialName("BlkioDeviceWriteIOps") public var blkioDeviceWriteIOps: List<ThrottleDevice>? = null,
        @SerialName("CpuPeriod") public var cpuPeriod: Long? = null,
        @SerialName("CpuQuota") public var cpuQuota: Long? = null,
        @SerialName("CpuRealtimePeriod") public var cpuRealtimePeriod: Long? = null,
        @SerialName("CpuRealtimeRuntime") public var cpuRealtimeRuntime: Long? = null,
        @SerialName("CpusetCpus") public var allowedCpus: String? = null,
        @SerialName("CpusetMems") public var allowedMEMs: String? = null,
        @SerialName("Devices") public var devices: List<DeviceMapping>? = null,
        @SerialName("DeviceCgroupRules") public var deviceCgroupRules: List<String>? = null,
        @SerialName("DeviceRequests") public var deviceRequests: List<DeviceRequest>? = null,
        @SerialName("KernelMemory") public var kernelMemory: Long? = null,
        @SerialName("KernelMemoryTCP") public var kernelMemoryTcp: Long? = null,
        @SerialName("MemoryReservation") public var memoryReservation: Long? = null,
        @SerialName("MemorySwap") public var memorySwap: Long? = null,
        @SerialName("MemorySwappiness") public var memorySwappiness: Long? = null,
        @SerialName("NanoCpus") public var nanoCpus: Long? = null,
        @SerialName("OomKillDisable") public var disabledOOMKiller: Boolean? = null,
        @SerialName("Init") public var init: Boolean? = null,
        @SerialName("PidsLimit") public var pidsLimit: Long? = null,
        @SerialName("Ulimits") public var resourcesLimit: List<ResourceLimit>? = null,
        @SerialName("CpuCount") public var cpuCount: Long? = null,
        @SerialName("CpuPercent") public var cpuPercent: Long? = null,
        @SerialName("IOMaximumIOps") public var ioMaximumIOps: Long? = null,
        @SerialName("IOMaximumBandwidth") public var ioMaximumBandwidth: Long? = null,
        // TODO provide better way to apply binds
        @SerialName("Binds") public var binds: List<String>? = null,
        @SerialName("ContainerIDFile") public var containerIDFile: String? = null,
        @SerialName("LogConfig") public var logConfig: LogConfig? = null,
        @SerialName("NetworkMode") public var networkMode: String? = null,
        @SerialName("PortBindings") public var portBindings:
            @Serializable(with = PortBindingsSerializer::class)
            Map<ExposedPort, List<PortBinding>?>? = null,
        @SerialName("RestartPolicy") public var restartPolicy: RestartPolicy? = null,
        @SerialName("AutoRemove") public var autoRemove: Boolean? = null,
        @SerialName("VolumeDriver") public var volumeDriver: String? = null,
        // TODO provide a better way to apply volumes
        @SerialName("VolumesFrom") public var volumesFrom: String? = null,
        @SerialName("Mounts") public var mounts: List<Mount> = emptyList(),
        @SerialName("CapAdd") public var capAdd: List<String>? = null,
        @SerialName("CapDrop") public var capDrop: List<String>? = null,
        // TODO provide constants for possible cgroupns mode values
        @SerialName("CgroupnsMode") public var cgroupnsMode: String? = null,
        @SerialName("Dns") public var dnsServers: List<String>? = null,
        @SerialName("DnsOptions") public var dnsOptions: List<String>? = null,
        @SerialName("DnsSearch") public var dnsSearch: List<String>? = null,
        // TODO allow use pairs to apply extra hosts
        @SerialName("ExtraHosts") public var extraHosts: List<String>? = null,
        @SerialName("GroupAdd") public var groupAdd: List<String>? = null,
        // TODO provide constants for possible ipc mode values
        @SerialName("IpcMode") public var ipcMode: String? = null,
        @SerialName("Cgroup") public var cgroup: String? = null,
        @SerialName("Links") public var links: List<String>? = null,
        @SerialName("OomScoreAdj") public var oomScoreAdj: Int? = null,
        // TODO provide a batter way to apply pid mode
        @SerialName("PidMode") public var pidMode: String? = null,
        @SerialName("Privileged") public var privileged: Boolean? = null,
        @SerialName("PublishAllPorts") public var publishAllPorts: Boolean? = null,
        @SerialName("ReadonlyRootFs") public var readonlyRootFs: Boolean? = null,
        @SerialName("SecurityOpt") public var securityOpt: List<String>? = null,
        @SerialName("StorageOpt") public var storageOpt: Map<String, String?>? = null,
        @SerialName("Tmpfs") public var tmpfs: Map<String, String?>? = null,
        @SerialName("UTSMode") public var utsMode: String? = null,
        @SerialName("UsernsMode") public var userNamespaceMode: String? = null,
        @SerialName("ShmSize") public var shmSize: Int? = null,
        @SerialName("Sysctls") public var sysctls: Map<String, String?>? = null,
        @SerialName("Runtime") public var runtime: String? = null,
        @SerialName("ConsoleSize") public var consoleSize: IntArray? = null,
        // TODO provide constants for possible isolation values
        @SerialName("Isolation") public var isolation: String? = null,
        @SerialName("MaskedPaths") public var maskedPaths: List<String>? = null,
        @SerialName("ReadonlyPaths") public var readonlyPaths: List<String>? = null,
    )

public fun HostConfig.portBindings(
    exposedPort: ExposedPort,
    portBindings: List<PortBinding>,
) {
    this.portBindings = this.portBindings.orEmpty() + mapOf(exposedPort to portBindings)
}

public fun HostConfig.portBindings(
    exposedPort: UShort,
    portBindings: List<PortBinding>,
) {
    this.portBindings(ExposedPort(exposedPort), portBindings)
}

public fun HostConfig.portBindings(
    exposedPort: ExposedPort,
    portBindingBuilder: MutableList<PortBinding>.() -> Unit = {},
) {
    this.portBindings(exposedPort, buildList(portBindingBuilder))
}

public fun HostConfig.portBindings(
    exposedPort: UShort,
    portBindingBuilder: MutableList<PortBinding>.() -> Unit = {},
) {
    this.portBindings(exposedPort, buildList(portBindingBuilder))
}
