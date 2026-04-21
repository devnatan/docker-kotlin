package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sentinel value Docker uses in `uint64` fields (e.g. memory/pids limits) to
 * indicate "unlimited". Equivalent to [ULong.MAX_VALUE].
 *
 * ```
 * if (stats.memoryStats?.limit == Unlimited) { ... }
 * ```
 */
public const val Unlimited: ULong = ULong.MAX_VALUE

/**
 * Container resource usage statistics.
 *
 * Fields are nullable because Docker returns different subsets depending
 * on the container platform (Linux vs Windows) and state (running vs stopped).
 *
 * Counter values are modeled as [ULong] to match Docker's `uint64` API types —
 * some fields (e.g. memory/pids limits) use the `uint64` max value as a sentinel
 * for "unlimited".
 */
@Serializable
public data class ContainerStats internal constructor(
    @SerialName("read") public val read: String,
    @SerialName("preread") public val preread: String? = null,
    @SerialName("name") public val name: String? = null,
    @SerialName("id") public val id: String? = null,
    @SerialName("num_procs") public val numProcs: ULong? = null,
    @SerialName("pids_stats") public val pidsStats: PidsStats? = null,
    @SerialName("cpu_stats") public val cpuStats: CpuStats? = null,
    @SerialName("precpu_stats") public val precpuStats: CpuStats? = null,
    @SerialName("memory_stats") public val memoryStats: MemoryStats? = null,
    @SerialName("blkio_stats") public val blkioStats: BlkioStats? = null,
    @SerialName("networks") public val networks: Map<String, NetworkStats>? = null,
    @SerialName("storage_stats") public val storageStats: StorageStats? = null,
)

@Serializable
public data class PidsStats internal constructor(
    @SerialName("current") public val current: ULong? = null,
    @SerialName("limit") public val limit: ULong? = null,
)

@Serializable
public data class CpuStats internal constructor(
    @SerialName("cpu_usage") public val cpuUsage: CpuUsage? = null,
    @SerialName("system_cpu_usage") public val systemCpuUsage: ULong? = null,
    @SerialName("online_cpus") public val onlineCpus: ULong? = null,
    @SerialName("throttling_data") public val throttlingData: ThrottlingData? = null,
)

@Serializable
public data class CpuUsage internal constructor(
    @SerialName("total_usage") public val totalUsage: ULong? = null,
    @SerialName("usage_in_kernelmode") public val usageInKernelmode: ULong? = null,
    @SerialName("usage_in_usermode") public val usageInUsermode: ULong? = null,
    @SerialName("percpu_usage") public val percpuUsage: List<ULong>? = null,
)

@Serializable
public data class ThrottlingData internal constructor(
    @SerialName("periods") public val periods: ULong? = null,
    @SerialName("throttled_periods") public val throttledPeriods: ULong? = null,
    @SerialName("throttled_time") public val throttledTime: ULong? = null,
)

@Serializable
public data class MemoryStats internal constructor(
    @SerialName("usage") public val usage: ULong? = null,
    @SerialName("max_usage") public val maxUsage: ULong? = null,
    @SerialName("limit") public val limit: ULong? = null,
    @SerialName("failcnt") public val failcnt: ULong? = null,
    @SerialName("stats") public val stats: Map<String, ULong>? = null,
    @SerialName("commitbytes") public val commitBytes: ULong? = null,
    @SerialName("commitpeakbytes") public val commitPeakBytes: ULong? = null,
    @SerialName("privateworkingset") public val privateWorkingSet: ULong? = null,
)

@Serializable
public data class BlkioStats internal constructor(
    @SerialName("io_service_bytes_recursive") public val ioServiceBytesRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("io_serviced_recursive") public val ioServicedRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("io_queue_recursive") public val ioQueueRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("io_service_time_recursive") public val ioServiceTimeRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("io_wait_time_recursive") public val ioWaitTimeRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("io_merged_recursive") public val ioMergedRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("io_time_recursive") public val ioTimeRecursive: List<BlkioStatsEntry>? = null,
    @SerialName("sectors_recursive") public val sectorsRecursive: List<BlkioStatsEntry>? = null,
)

@Serializable
public data class BlkioStatsEntry internal constructor(
    @SerialName("major") public val major: ULong? = null,
    @SerialName("minor") public val minor: ULong? = null,
    @SerialName("op") public val op: String? = null,
    @SerialName("value") public val value: ULong? = null,
)

@Serializable
public data class NetworkStats internal constructor(
    @SerialName("rx_bytes") public val rxBytes: ULong? = null,
    @SerialName("rx_packets") public val rxPackets: ULong? = null,
    @SerialName("rx_errors") public val rxErrors: ULong? = null,
    @SerialName("rx_dropped") public val rxDropped: ULong? = null,
    @SerialName("tx_bytes") public val txBytes: ULong? = null,
    @SerialName("tx_packets") public val txPackets: ULong? = null,
    @SerialName("tx_errors") public val txErrors: ULong? = null,
    @SerialName("tx_dropped") public val txDropped: ULong? = null,
)

@Serializable
public data class StorageStats internal constructor(
    @SerialName("read_count_normalized") public val readCountNormalized: ULong? = null,
    @SerialName("read_size_bytes") public val readSizeBytes: ULong? = null,
    @SerialName("write_count_normalized") public val writeCountNormalized: ULong? = null,
    @SerialName("write_size_bytes") public val writeSizeBytes: ULong? = null,
)
