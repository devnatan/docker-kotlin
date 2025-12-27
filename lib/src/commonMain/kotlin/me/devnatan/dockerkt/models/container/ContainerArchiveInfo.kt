package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class ContainerArchiveInfo(
    val name: String,
    val size: Long,
    val mode: Int,
    @SerialName("mtime") val modifiedAtRaw: String,
    val linkTarget: String = "",
)

@OptIn(ExperimentalTime::class)
public val ContainerArchiveInfo.modifiedAt: Instant
    get() = Instant.parse(modifiedAtRaw)
