@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.image

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class ImageSummary(
    @SerialName("Id") @Required public val id: String,
    @SerialName("ParentId") @Required public val parentId: String,
    @SerialName("RepoTags") @Required public val repositoryTags: List<String>,
    @SerialName("RepoDigests") @Required public val repositoryDigests: List<String>,
    @SerialName("Created") @Required public val created: Int,
    @SerialName("Size") @Required public val size: Long,
    @SerialName("SharedSize") @Required public val sharedSize: Int,
    @SerialName("Labels") @Required public val labels: Map<String, String>?,
    @SerialName("Containers") @Required public val containers: Int,
)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val ImageSummary.created: Instant
    get() = Instant.fromEpochMilliseconds(created.toLong())
