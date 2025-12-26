@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.devnatan.dockerkt.models.GraphDriverData
import me.devnatan.dockerkt.models.container.ContainerConfig
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class Image(
    @SerialName("Id") public val id: String,
    @SerialName("Parent") public val parent: String,
    @SerialName("Comment") public val comment: String,
    @SerialName("Created") public val created: String,
    @SerialName("Container") public val container: String,
    @SerialName("DockerVersion") public val dokerVersion: String,
    @SerialName("Author") public val author: String,
    @SerialName("Architecture") public val arch: String,
    @SerialName("Os") public val os: String,
    @SerialName("Size") public val size: Long,
    @SerialName("VirtualSize") public val virtualSize: Long,
    @SerialName("GraphDriver") public val graphDriver: GraphDriverData,
    @SerialName("RootFS") public val rootFS: ImageRootFs,
    @SerialName("RepoTags") public val repositoryTags: List<String>? = null,
    @SerialName("RepoDigests") public val repositoryDigests: List<String>? = null,
    @SerialName("ContainerConfig") public val containerConfig: ContainerConfig? = null,
    @SerialName("Config") public val config: ContainerConfig? = null,
    @SerialName("OsVersion") public val osVersion: String? = null,
    @SerialName("Metadata") public val metadata: ImageMetadata? = null,
)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Image.created: Instant
    get() = Instant.parse(created)

@Serializable
public data class ImageMetadata(
    @SerialName("LastTagImage") public val lastTagTime: String? = null,
)
