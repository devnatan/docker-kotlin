@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.secret

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Secrets are sensitive data that can be used by Docker services.
 *
 * @property id The id of the secret.
 * @property createdAt The instant this secret was created.
 * @property updatedAt The instant this secret was updated.
 */
@Serializable
public data class Secret(
    @SerialName("ID") public val id: String,
    @SerialName("CreatedAt") public val createdAtRaw: String,
    @SerialName("UpdatedAt") public val updatedAtRaw: String,
    // TODO version
) {
    val createdAt: Instant by lazy { Instant.parse(createdAtRaw) }
    val updatedAt: Instant by lazy { Instant.parse(updatedAtRaw) }
}
