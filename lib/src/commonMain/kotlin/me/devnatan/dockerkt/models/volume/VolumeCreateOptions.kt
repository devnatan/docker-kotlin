package me.devnatan.dockerkt.models.volume

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Volume configuration.
 *
 * @property name The volume's name.
 * @property driver Name of the volume driver to use.
 * @property driverOpts A mapping of driver options and values.
 * @property labels User-defined key/value metadata.
 */
@Serializable
public data class VolumeCreateOptions(
    @SerialName("Name") public var name: String? = null,
    @SerialName("Driver") public var driver: String? = null,
    @SerialName("DriverOpts") public var driverOpts: Map<String, String>? = null,
    @SerialName("Labels") public var labels: Map<String, String>? = null,
)
