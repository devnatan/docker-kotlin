package me.devnatan.dockerkt.models.network

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

/**
 * Network list filters.
 *
 * @property dangling When set to `true`, returns all networks that are not in use by a container.
 * When set to `false`, only networks that are in use by one or more containers are returned.
 * @property driver Matches a network's driver.
 * @property id Matches all or part of a network ID.
 * @property name Matches all or part of a network name.
 * @property scope Filters networks by scope.
 * @property type Filters networks by type. The `custom` type returns all user-defined networks.
 * @see NetworkResource.list
 */
@Serializable
public data class NetworkListFilters
    @JvmOverloads
    public constructor(
        public var dangling: Boolean = false,
        public var driver: String? = null,
        public var id: String? = null,
        public var name: String? = null,
        public var scope: String? = null,
        public var type: String? = null,
    )
