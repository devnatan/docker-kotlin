package me.devnatan.dockerkt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
public data class HealthConfig(
    @SerialName("Test") public var test: List<String>? = null,
    @SerialName("Interval") public var interval: ULong? = null,
    @SerialName("Timeout") public var timeout: ULong? = null,
    @SerialName("Retries") public var retries: Int? = null,
    @SerialName("StartPeriod") public var startPeriod: ULong? = null,
)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public var HealthConfig.interval: Duration?
    get() = interval?.toLong()?.toDuration(DurationUnit.NANOSECONDS)
    set(value) {
        interval = value?.inWholeNanoseconds?.toULong()
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public var HealthConfig.timeout: Duration?
    get() = timeout?.toLong()?.toDuration(DurationUnit.NANOSECONDS)
    set(value) {
        timeout = value?.inWholeNanoseconds?.toULong()
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public var HealthConfig.startPeriod: Duration?
    get() = startPeriod?.toLong()?.toDuration(DurationUnit.NANOSECONDS)
    set(value) {
        startPeriod = value?.inWholeNanoseconds?.toULong()
    }
