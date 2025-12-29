package me.devnatan.dockerkt.resource.exec

import me.devnatan.dockerkt.models.exec.ExecCreateOptions
import me.devnatan.dockerkt.models.exec.ExecStartOptions
import me.devnatan.dockerkt.models.exec.ExecStartResult
import me.devnatan.dockerkt.resource.container.ContainerNotFoundException
import me.devnatan.dockerkt.resource.container.ContainerNotRunningException

/**
 * Creates a new container.
 *
 * @param id The container id to execute the command.
 * @param options Options to customize the container creation.
 * @throws ContainerNotFoundException If container instance is not found.
 * @throws ContainerNotRunningException If the container is not running.
 */
public suspend inline fun ExecResource.create(
    id: String,
    options: ExecCreateOptions.() -> Unit,
): String = create(id, ExecCreateOptions().apply(options))

/**
 * Starts a previously set up exec instance.
 *
 * If detach is true, this endpoint returns immediately after starting the command.
 * Otherwise, it sets up an interactive session with the command.
 *
 * @param id The exec instance id to be started.
 * @param options Options to customize the exec start.
 * @throws ExecNotFoundException If exec instance is not found.
 * @throws ContainerNotRunningException If the container in which the exec instance was created is not running.
 */
public suspend inline fun ExecResource.start(
    id: String,
    options: ExecStartOptions.() -> Unit = {},
): ExecStartResult = start(id, ExecStartOptions().apply(options))
