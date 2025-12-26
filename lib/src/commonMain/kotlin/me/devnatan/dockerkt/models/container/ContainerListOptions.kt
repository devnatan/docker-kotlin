package me.devnatan.dockerkt.models.container

import kotlinx.serialization.Serializable

@Serializable
public data class ContainerListOptions(
    public var all: Boolean? = null,
    public var limit: Int? = null,
    public var size: Boolean? = null,
    public var filters: Filters? = null,
) {
    @Serializable
    public data class Filters(
        public var ancestor: List<String>? = null, // TODO image pattern
        public var before: List<String>? = null,
        public var expose: List<String>? = null, // TODO ExposedPort type
        public var exited: List<Int>? = null,
        public var health: List<String>? = null, // TODO only possible values
        public var id: List<String>? = null,
        public var isolation: List<String>? = null, // TODO only possible values
        public var isTask: List<Boolean>? = null,
        public var label: List<String>? = null, // TODO encoded url format
        public var name: List<String>? = null,
        public var network: List<String>? = null,
        public var publish: List<String>? = null, // TODO ExposedPort type
        public var since: List<String>? = null,
        public var status: List<String>? = null, // TODO use ContainerStatus object
        public var volume: List<String>? = null,
    )
}

public inline fun ContainerListOptions.filters(filters: ContainerListOptions.Filters.() -> Unit) {
    this.filters = ContainerListOptions.Filters().apply(filters)
}
