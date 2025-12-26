package me.devnatan.dockerkt.models

import kotlinx.serialization.Serializable

@Serializable
public data class ProcessConfig internal constructor(
    val privileged: Boolean,
    val user: String? = null,
    val tty: Boolean,
    val entrypoint: String,
    val arguments: List<String>,
)
