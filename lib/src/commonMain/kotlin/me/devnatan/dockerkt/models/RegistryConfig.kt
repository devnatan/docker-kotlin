package me.devnatan.dockerkt.models

import kotlinx.serialization.Serializable

/**
 * Configuration for Docker registry authentication.
 *
 * @property username The username for authentication with the Docker registry.
 * @property password The password for authentication with the Docker registry.
 */
@Serializable
public data class RegistryConfig(
    val username: String,
    val password: String,
)
