package me.devnatan.dockerkt.models.secret

import kotlinx.serialization.Serializable

/**
 * Filters to apply to the secrets list process.
 *
 * @property id The secret id.
 * @property name The secret name.
 * @property label Secret labels.
 */
@Serializable
public data class SecretListFilters(
    public var id: String? = null,
    public var label: String? = null,
    public var name: String? = null,
)
