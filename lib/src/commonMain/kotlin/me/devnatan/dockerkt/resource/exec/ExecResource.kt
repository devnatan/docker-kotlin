package me.devnatan.dockerkt.resource.exec

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.IdOnlyResponse
import me.devnatan.dockerkt.models.ResizeTTYOptions
import me.devnatan.dockerkt.models.exec.ExecCreateOptions
import me.devnatan.dockerkt.models.exec.ExecInspectResponse
import me.devnatan.dockerkt.models.exec.ExecStartOptions
import me.devnatan.dockerkt.resource.ResourcePaths.CONTAINERS
import me.devnatan.dockerkt.resource.container.ContainerNotFoundException
import me.devnatan.dockerkt.resource.container.ContainerNotRunningException
import kotlin.jvm.JvmSynthetic

/**
 * Exec runs new commands inside running containers.
 *
 * This resource is equivalent to calling `docker exec` on CLI.
 *
 * [Exec Command-line reference](https://docs.docker.com/engine/reference/commandline/exec/)
 */
public class ExecResource internal constructor(
    private val httpClient: HttpClient,
) {
    private companion object {
        const val BASE_PATH = "/exec"
    }

    /**
     * Runs a command inside a running container.
     *
     * @param id The container id to execute the command.
     * @param options Exec instance command options.
     * @throws ContainerNotFoundException If container instance is not found.
     * @throws ContainerNotRunningException If the container is not running.
     */
    @JvmSynthetic
    public suspend fun create(
        id: String,
        options: ExecCreateOptions,
    ): String =
        requestCatching(
            HttpStatusCode.NotFound to { cause ->
                ContainerNotFoundException(
                    cause,
                    id,
                )
            },
            HttpStatusCode.Conflict to { cause ->
                ContainerNotRunningException(
                    cause,
                    id,
                )
            },
        ) {
            httpClient.post("$CONTAINERS/$id/exec") {
                setBody(options)
            }
        }.body<IdOnlyResponse>().id

    /**
     * Inspects an exec instance and returns low-level information about it.
     * `docker exec inspect`
     *
     * @param id Exec instance unique identifier.
     * @throws ExecNotFoundException If exec instance is not found.
     */
    public suspend fun inspect(id: String): ExecInspectResponse =
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ExecNotFoundException(exception, id) },
        ) {
            httpClient.get("$BASE_PATH/$id/json")
        }.body()

    /**
     * Starts a previously set up exec instance.
     *
     * If [ExecStartOptions.detach] from [options] is set to `true`, it'll return immediately after starting the
     * command. Otherwise, it will set up and interactive session with the command.
     *
     * @param id Exec instance unique identifier.
     * @throws ExecNotFoundException If exec instance is not found.
     * @throws ContainerNotRunningException If the container in which the exec instance was created is not running.
     */
    public suspend fun start(
        id: String,
        options: ExecStartOptions,
    ) {
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ExecNotFoundException(exception, id) },
            HttpStatusCode.Conflict to { exception -> ContainerNotRunningException(exception, null) },
        ) {
            httpClient.post("$BASE_PATH/$id/start") {
                setBody(options)
            }
        }
    }

    /**
     * Resizes a TTY session used by an exec instance.
     *
     * Note: Will work only if `tty` option was specified during creation and start of the exec instance.
     *
     * @param id Exec unique identifier.
     * @param options Options for TTY resizing.
     * @throws ExecNotFoundException If exec instance is not found.
     */
    public suspend fun resize(
        id: String,
        options: ResizeTTYOptions,
    ) {
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ExecNotFoundException(exception, id) },
        ) {
            httpClient.post("$BASE_PATH/$id/resize") {
                setBody(options)
            }
        }
    }
}
