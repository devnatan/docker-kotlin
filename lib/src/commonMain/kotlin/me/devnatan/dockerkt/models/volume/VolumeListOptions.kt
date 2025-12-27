package me.devnatan.dockerkt.models.volume

import kotlinx.serialization.Serializable

/**
 * Volume list filters.
 *
 * @property dangling When set to `true`, returns all volumes that are not in use by a container.
 *                    When set to `false`, only volumes that are in use by one or more containers are returned.
 * @property driver Matches a network's driver.
 * @property label matches based on the presence of a label alone or a label and a value.
 * @property name Matches all or part of a network name.
 */
@Serializable
public data class VolumeListOptions
    @PublishedApi
    internal constructor(
        public var dangling: Boolean? = null,
        public var driver: String? = null,
        public var label: String? = null,
        public var name: String? = null,
    )
