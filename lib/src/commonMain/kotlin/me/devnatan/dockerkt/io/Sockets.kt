@file:JvmName("SocketUtils")

package me.devnatan.dockerkt.io

import kotlin.jvm.JvmName

internal const val ENCODED_HOSTNAME_SUFFIX = ".socket"

internal const val DOCKER_SOCKET_PORT = 2375
internal const val UNIX_SOCKET_PREFIX = "unix://"
internal const val HTTP_SOCKET_PREFIX = "tcp://"

// unix:///var/run/docker.sock
public const val DEFAULT_DOCKER_UNIX_SOCKET: String = "$UNIX_SOCKET_PREFIX/var/run/docker.sock"

// tcp://localhost:2375
public const val DEFAULT_DOCKER_HTTP_SOCKET: String = "${HTTP_SOCKET_PREFIX}localhost:$DOCKER_SOCKET_PORT"

internal fun isUnixSocket(input: String): Boolean = input.startsWith(UNIX_SOCKET_PREFIX)
