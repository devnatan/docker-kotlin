@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.system

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

public data class MonitorEventsOptions(
    public var since: String? = null,
    public var until: String? = null,
    public val filters: MutableMap<String, MutableList<String>> = mutableMapOf(),
) {
    /**
     * Filters event by one or more types.
     *
     * @param types The event types to filter.
     */
    public fun filterByType(vararg types: EventType): MonitorEventsOptions =
        apply {
            types.forEach { type -> addFilter("type", type.name.lowercase()) }
        }

    /**
     * Adds a new filter to the monitor events.
     *
     * @param key The filter key.
     * @param value The filter value.
     */
    public fun addFilter(
        key: String,
        value: String,
    ) {
        filters.getOrPut(key, ::mutableListOf).add(value)
    }
}

public fun MonitorEventsOptions.setSince(since: Instant?) {
    this.since = since?.toString()
}

public fun MonitorEventsOptions.setUntil(until: Instant?) {
    this.until = until?.toString()
}
