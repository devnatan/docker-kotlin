@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.devnatan.dockerkt.models.GraphDriverData
import me.devnatan.dockerkt.models.HealthConfig
import me.devnatan.dockerkt.models.container.VolumesSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class Image(
    @SerialName("Id") public val id: String,
    @SerialName("RepoTags") public val repositoryTags: List<String> = emptyList(),
    @SerialName("RepoDigests") public val repositoryDigests: List<String> = emptyList(),
    @SerialName("Comment") public val comment: String? = null,
    @SerialName("Created") public val created: String? = null,
    @SerialName("Author") public val author: String? = null,
    @SerialName("Config") public val config: ImageConfig,
    @SerialName("Architecture") public val architecture: String? = null,
    @SerialName("Variant") public val variant: String? = null,
    @SerialName("Os") public val os: String,
    @SerialName("OsVersion") public val osVersion: String? = null,
    @SerialName("Size") public val size: Long,
    @SerialName("GraphDriver") public val graphDriver: GraphDriverData,
    @SerialName("RootFS") public val rootFS: ImageRootFs,
    @SerialName("Metadata") public val metadata: ImageMetadata? = null,
)

@Serializable
public data class ImageConfig(
    @SerialName("User") public val user: String? = null,
    @SerialName("Env") public val env: List<String> = emptyList(),
    @SerialName("Cmd") public val command: List<String>? = null,
    @SerialName("Volumes") public val volumes:
        @Serializable(with = VolumesSerializer::class)
        List<String>? = emptyList(),
    @SerialName("WorkingDir") public val workingDir: String? = null,
    @SerialName("Entrypoint") public val entrypoint: List<String>? = null,
    @SerialName("OnBuild") public val onBuild: List<String>? = null,
    @SerialName("Labels") public val labels: Map<String, String> = emptyMap(),
    @SerialName("StopSignal") public val stopSignal: String? = null,
    @SerialName("Shell") public val shell: List<String>? = null,
    @SerialName("Healthcheck") public val healthcheck: HealthConfig? = null,
)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Image.created: Instant?
    get() = created?.let(Instant::parse)

@Serializable
public data class ImageMetadata(
    @SerialName("LastTagImage") public val lastTagTime: String? = null,
)
