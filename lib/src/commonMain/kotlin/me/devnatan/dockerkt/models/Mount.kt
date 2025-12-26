package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Mount internal constructor(
    @SerialName("Target") val target: String? = null,
    @SerialName("Source") val source: String? = null,
    @SerialName("Type") val type: Type,
    @SerialName("ReadOnly") val readonly: Boolean = false,
    @SerialName("Consistency") val consistency: String? = null,
    @SerialName("BindOptions") val bindOptions: MountBindOptions? = null,
    @SerialName("VolumeOptions") val volumeOptions: MountVolumeOptions? = null,
    @SerialName("TmpfsOptions") val tmpfsOptions: MountTmpfsOptions? = null,
) {
    @Serializable
    public enum class Type {
        @SerialName("bind")
        Bind,

        @SerialName("volume")
        Volume,

        @SerialName("tmpfs")
        Tmpfs,

        @SerialName("npipe")
        NamedPipe,
    }
}

/**
 * @property propagation A propagation mode for the mount.
 * @property nonRecursive Disable recursive bind mount.
 */
@Serializable
public data class MountBindOptions(
    @SerialName("Propagation") val propagation: Propagation? = null,
    @SerialName("NonRecursive") val nonRecursive: Boolean? = null,
) {
    /**
     * Bind propagation refers to whether or not mounts created within a given bind-mount can be
     * propagated to replicas of a given [Mount].
     */
    @Serializable
    public enum class Propagation {
        /**
         * The mount is private. Sub-mounts within it are not exposed to replica mounts, and
         * sub-mounts of replica mounts are not exposed to the original mount.
         */
        @SerialName("private")
        Private,

        /**
         * Sub-mounts of the original mount are exposed to replica mounts, and sub-mounts of replica
         * mounts are also propagated to the original mount.
         */
        @SerialName("shared")
        Shared,

        /**
         * Similar to a [Shared] mount, but only in one direction. If the original mount exposes a
         * sub-mount, the replica mount can see it. However, if the replica mount exposes a
         * sub-mount, the original mount cannot  see it.
         */
        @SerialName("slave")
        Slave,

        /**
         * The default. The same as [Private], meaning that no mount points anywhere within the
         * original or replica mount points propagate in either direction.
         */
        @SerialName("rprivate")
        RPrivate,

        /**
         * The same as [Shared], but the propagation also extends to and from mount points nested
         * within any of the original or replica mount points.
         */
        @SerialName("rshared")
        RShared,

        /**
         * The same as [Slave], but the propagation also extends to and from mount points nested
         * within any of the original or replica mount points.
         */
        @SerialName("rslave")
        RSlave,
    }
}

@Serializable
public data class MountVolumeOptions(
    @SerialName("NoCopy") val noCopy: Boolean? = null,
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
    @SerialName("DriverConfig") val driverConfig: MountVolumeOptionsDriverConfig? = null,
)

@Serializable
public data class MountVolumeOptionsDriverConfig(
    @SerialName("Name") val name: String? = null,
    @SerialName("Options") val options: Map<String, String> = emptyMap(),
)

/**
 * @property sizeInBytes The size for the tmpfs mount in bytes.
 * @property mode The permission mode for the tmpfs mount.
 */
@Serializable
public data class MountTmpfsOptions(
    @SerialName("SizeBytes") val sizeInBytes: Long? = null,
    @SerialName("Mode") val mode: Int? = null,
)
