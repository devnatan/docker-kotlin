package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class ContainerArchiveInfo(
    @SerialName("name")
    val name: String,
    @SerialName("size")
    val size: Long,
    @SerialName("mode")
    val mode: Long,
    @SerialName("mtime")
    val modifiedAtMillis: String,
    @SerialName("linkTarget")
    val linkTarget: String = "",
)

@OptIn(ExperimentalTime::class)
public val ContainerArchiveInfo.modifiedAt: Instant
    get() = Instant.parse(modifiedAtMillis)
