package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container resource usage statistics as returned by the
 * `GET /containers/:id/stats` endpoint.
 *
 * Fields are nullable because Docker returns different subsets depending
 * on the container platform (Linux vs Windows) and state (running vs stopped).
 */
@Serializable
public data class ContainerStats internal constructor(
    @SerialName("read") public val read: String,
    @SerialName("preread") public val preread: String? = null,
    @SerialName("name") public val name: String? = null,
    @SerialName("id") public val id: String? = null,
    @SerialName("num_procs") public val numProcs: Long? = null,
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
    @SerialName("current") public val current: Long? = null,
    @SerialName("limit") public val limit: Long? = null,
)

@Serializable
public data class CpuStats internal constructor(
    @SerialName("cpu_usage") public val cpuUsage: CpuUsage? = null,
    @SerialName("system_cpu_usage") public val systemCpuUsage: Long? = null,
    @SerialName("online_cpus") public val onlineCpus: Long? = null,
    @SerialName("throttling_data") public val throttlingData: ThrottlingData? = null,
)

@Serializable
public data class CpuUsage internal constructor(
    @SerialName("total_usage") public val totalUsage: Long? = null,
    @SerialName("usage_in_kernelmode") public val usageInKernelmode: Long? = null,
    @SerialName("usage_in_usermode") public val usageInUsermode: Long? = null,
    @SerialName("percpu_usage") public val percpuUsage: List<Long>? = null,
)

@Serializable
public data class ThrottlingData internal constructor(
    @SerialName("periods") public val periods: Long? = null,
    @SerialName("throttled_periods") public val throttledPeriods: Long? = null,
    @SerialName("throttled_time") public val throttledTime: Long? = null,
)

@Serializable
public data class MemoryStats internal constructor(
    @SerialName("usage") public val usage: Long? = null,
    @SerialName("max_usage") public val maxUsage: Long? = null,
    @SerialName("limit") public val limit: Long? = null,
    @SerialName("failcnt") public val failcnt: Long? = null,
    @SerialName("stats") public val stats: Map<String, Long>? = null,
    @SerialName("commitbytes") public val commitBytes: Long? = null,
    @SerialName("commitpeakbytes") public val commitPeakBytes: Long? = null,
    @SerialName("privateworkingset") public val privateWorkingSet: Long? = null,
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
    @SerialName("major") public val major: Long? = null,
    @SerialName("minor") public val minor: Long? = null,
    @SerialName("op") public val op: String? = null,
    @SerialName("value") public val value: Long? = null,
)

@Serializable
public data class NetworkStats internal constructor(
    @SerialName("rx_bytes") public val rxBytes: Long? = null,
    @SerialName("rx_packets") public val rxPackets: Long? = null,
    @SerialName("rx_errors") public val rxErrors: Long? = null,
    @SerialName("rx_dropped") public val rxDropped: Long? = null,
    @SerialName("tx_bytes") public val txBytes: Long? = null,
    @SerialName("tx_packets") public val txPackets: Long? = null,
    @SerialName("tx_errors") public val txErrors: Long? = null,
    @SerialName("tx_dropped") public val txDropped: Long? = null,
)

@Serializable
public data class StorageStats internal constructor(
    @SerialName("read_count_normalized") public val readCountNormalized: Long? = null,
    @SerialName("read_size_bytes") public val readSizeBytes: Long? = null,
    @SerialName("write_count_normalized") public val writeCountNormalized: Long? = null,
    @SerialName("write_size_bytes") public val writeSizeBytes: Long? = null,
)
