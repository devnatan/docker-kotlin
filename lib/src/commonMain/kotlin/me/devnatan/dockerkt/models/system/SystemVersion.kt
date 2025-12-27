@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * System version information.
 *
 * @property platform Platform of the machine on which Docker is running.
 * @property components Information about system components.
 * @property version Current Docker daemon version.
 * @property apiVersion The default (and highest) API version that's supported by the daemon.
 * @property minApiVersion Minimum API version that's supported by the daemon.
 * @property gitCommit Git commit of the source code that was used to build the daemon.
 * @property goVersion Go version used to compile the daemon and the version of the Go runtime in use.
 * @property os Operating system that the daemon is running. Can be `"linux"` or `"windows"`.
 * @property arch The architecture that the daemon is running on.
 * @property kernelVersion The kernel version (`uname -r`) that the daemon is running on.
 * @property experimental If the daemon is started with experimental features enabled.
 * @property buildTimeRaw The date and time that the daemon was compiled.
 */
@Serializable
public data class SystemVersion internal constructor(
    @SerialName("Platform") val platform: Platform,
    @SerialName("Components") val components: List<Component>,
    @SerialName("Version") val version: String,
    @SerialName("ApiVersion") val apiVersion: String,
    @SerialName("MinAPIVersion") val minApiVersion: String,
    @SerialName("GitCommit") val gitCommit: String,
    @SerialName("GoVersion") val goVersion: String,
    @SerialName("Os") val os: String,
    @SerialName("Arch") val arch: String,
    @SerialName("KernelVersion") val kernelVersion: String? = null,
    @SerialName("Experimental") val experimental: Boolean = false,
    @SerialName("BuildTime") val buildTimeRaw: String,
) {
    /**
     * The date and time that the daemon was compiled as an [Instant].
     */
    public val buildTime: Instant by lazy { Instant.parse(buildTimeRaw) }

    /**
     * System version platform.
     *
     * @property name Name of the platform.
     */
    @Serializable
    public data class Platform internal constructor(
        @SerialName("Name") val name: String,
    )

    /**
     * System version component.
     *
     * @property name Name of the component.
     * @property version Version of the component.
     * @property details Key/value pairs of strings with additional information about the component. These values are
     *                   intended for informational purposes only, and their content is not defined, and not part of the
     *                   API specification. These messages can be printed by the client as information to the user.
     */
    @Serializable
    public data class Component internal constructor(
        @SerialName("Name") val name: String,
        @SerialName("Version") val version: String,
        @SerialName("Details") val details: Details? = null,
    ) {
        /**
         * System version component details.
         *
         * @property gitCommit Git commit of the source code that was used to build the component.
         */
        @Serializable
        public data class Details internal constructor(
            @SerialName("GitCommit") val gitCommit: String,
        )
    }
}
