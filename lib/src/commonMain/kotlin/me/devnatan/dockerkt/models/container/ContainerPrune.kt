package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ContainerPruneFilters(
    public var until: String? = null,
    public var label: String? = null,
) {
    public data class Builder(
        private val until: String? = null,
        private val label: String? = null,
    ) {
        public fun until(until: String?): Builder = copy(until = until)

        public fun label(label: String?): Builder = copy(label = label)

        public fun build(): ContainerPruneFilters =
            ContainerPruneFilters(
                until = until,
                label = label,
            )
    }
}

@Serializable
public data class ContainerPruneResult internal constructor(
    @SerialName("ContainersDeleted") public val deletedContainers: List<String>,
    @SerialName("SpaceReclaimed") public val reclaimedSpace: Long,
)
