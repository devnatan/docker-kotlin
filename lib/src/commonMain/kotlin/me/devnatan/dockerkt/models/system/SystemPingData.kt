package me.devnatan.dockerkt.models.system

import kotlinx.serialization.Serializable

/**
 * Data obtained when a successful ping is made by the client.
 *
 * @property apiVersion Max API version the server supports.
 *
 */
@Serializable
public data class SystemPingData internal constructor(
    val apiVersion: String,
    val builderVersion: String,
    val experimental: Boolean,
)
