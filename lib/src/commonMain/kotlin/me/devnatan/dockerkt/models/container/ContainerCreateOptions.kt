package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.devnatan.dockerkt.models.ExposedPort
import me.devnatan.dockerkt.models.ExposedPortProtocol
import me.devnatan.dockerkt.models.ExposedPortsSerializer
import me.devnatan.dockerkt.models.HealthConfig
import me.devnatan.dockerkt.models.HostConfig
import me.devnatan.dockerkt.models.network.NetworkingConfig
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
public data class ContainerCreateOptions(
    public var name: String? = null,
    @SerialName("Hostname") public var hostName: String? = null,
    @SerialName("Domainname") public var domainName: String? = null,
    @SerialName("User") public var user: String? = null,
    @SerialName("AttachStdin") public var attachStdin: Boolean? = null,
    @SerialName("ExposedPorts") public var exposedPorts:
        @Serializable(with = ExposedPortsSerializer::class)
        List<ExposedPort>? = null,
    @SerialName("Env") public var env: List<String>? = null,
    @SerialName("Cmd") public var command: List<String>? = null,
    @SerialName("Healthcheck") public var healthcheck: HealthConfig? = null,
    @SerialName("ArgsEscaped") public var escapedArgs: Boolean? = null,
    @SerialName("Image") public var image: String? = null,
    @SerialName("Volumes") public var volumes:
        @Serializable(with = VolumesSerializer::class)
        List<String>? = null,
    @SerialName("WorkingDir") public var workingDirectory: String? = null,
    @SerialName("Entrypoint") public var entrypoint: List<String>? = null,
    @SerialName("NetworkDisabled") public var disabledNetwork: Boolean? = null,
    @SerialName("MacAddress") public var macAddress: String? = null,
    @SerialName("OnBuild") public var buildMetadata: List<String>? = null,
    @SerialName("Labels") public var labels: Map<String, String>? = null,
    @SerialName("StopSignal") public var stopSignal: String? = null,
    @SerialName("StopTimeout") public var stopTimeout: Int? = null,
    @SerialName("Shell") public var shell: List<String>? = null,
    @SerialName("HostConfig") public var hostConfig: HostConfig? = null,
    @SerialName("NetworkingConfig") public var networkingConfig: NetworkingConfig? = null,
    @SerialName("Tty") public var tty: Boolean? = null,
)

public fun ContainerCreateOptions.exposedPort(port: UShort) {
    this.exposedPort(port, ExposedPortProtocol.TCP)
}

public fun ContainerCreateOptions.exposedPort(
    port: UShort,
    protocol: ExposedPortProtocol,
) {
    this.exposedPorts = exposedPorts.orEmpty() + listOf(ExposedPort(port, protocol))
}

public fun ContainerCreateOptions.healthcheck(block: HealthConfig.() -> Unit) {
    this.healthcheck = HealthConfig().apply(block)
}

public fun ContainerCreateOptions.hostConfig(block: HostConfig.() -> Unit) {
    this.hostConfig = HostConfig().apply(block)
}

public fun ContainerCreateOptions.volume(string: String) {
    this.volumes = this.volumes.orEmpty() + string
}

public fun ContainerCreateOptions.networkingConfig(block: NetworkingConfig.() -> Unit) {
    this.networkingConfig = NetworkingConfig().apply(block)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public var ContainerCreateOptions.stopTimeout: Duration?
    get() = stopTimeout?.toDuration(DurationUnit.NANOSECONDS)
    set(value) {
        stopTimeout = value?.inWholeNanoseconds?.toInt()
    }
