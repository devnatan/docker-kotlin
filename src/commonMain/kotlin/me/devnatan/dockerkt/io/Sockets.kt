@file:JvmName("SocketUtils")

package me.devnatan.dockerkt.io

import kotlin.jvm.JvmName

internal const val EncodedHostnameSuffix = ".socket"
internal const val MaxDnsLabelLength = 63

internal const val DockerSocketPort = 2375
internal const val UnixSocketPrefix = "unix://"
internal const val HttpSocketPrefix = "tcp://"

// unix:///var/run/docker.sock
public const val DefaultDockerUnixSocket: String = "$UnixSocketPrefix/var/run/docker.sock"

// tcp://localhost:2375
public const val DefaultDockerHttpSocket: String = "${HttpSocketPrefix}localhost:$DockerSocketPort"

internal fun isUnixSocket(input: String): Boolean = input.startsWith(UnixSocketPrefix)

@OptIn(ExperimentalStdlibApi::class)
internal fun encodeSocketPathHostname(socketPath: String): String =
    socketPath
        .encodeToByteArray()
        .toHexString()
        .chunked(MaxDnsLabelLength)
        .joinToString(".") + EncodedHostnameSuffix

@OptIn(ExperimentalStdlibApi::class)
internal fun decodeSocketPathHostname(hostname: String): String =
    hostname
        .substring(0, hostname.indexOf(EncodedHostnameSuffix))
        .replace(".", "")
        .hexToByteArray()
        .decodeToString()
