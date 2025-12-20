package me.devnatan.dockerkt.util

import kotlinx.serialization.json.Json

public val DockerKotlinJson: Json =
    Json {
        ignoreUnknownKeys = true
        allowStructuredMapKeys = true
        explicitNulls = false
    }

public fun toJsonEncodedString(value: Any): String = DockerKotlinJson.encodeToString(value)

public fun fromJsonEncodedString(value: String): Map<String, String?> = DockerKotlinJson.decodeFromString(value)
