package me.devnatan.dockerkt.resource.system

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.system.Event
import me.devnatan.dockerkt.models.system.MonitorEventsOptions
import me.devnatan.dockerkt.models.system.SystemInfo
import me.devnatan.dockerkt.models.system.SystemPingData
import me.devnatan.dockerkt.models.system.SystemVersion
import kotlin.jvm.JvmOverloads

/**
 * Resource responsible for getting information from the operating system and the Docker daemon it is running on.
 */
public class SystemResource internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    /**
     * Gets system-wide information about the Docker daemon.
     *
     * Corresponding CLI command:
     * ```sh
     * $ docker info
     * ```
     *
     * @throws me.devnatan.dockerkt.DockerResourceException If an error occurs in the request.
     */
    public suspend fun info(): SystemInfo =
        requestCatching {
            httpClient.get("/info")
        }.body()

    /**
     * Gets the version of Docker that is running and information about the system that Docker is running on.
     *
     * Corresponding CLI command:
     * ```sh
     * $ docker version
     * ```
     *
     * @throws me.devnatan.dockerkt.DockerResourceException If an error occurs in the request.
     */
    public suspend fun version(): SystemVersion =
        requestCatching {
            httpClient.get("/version")
        }.body()

    /**
     * Pings the server completing successfully if the server is accessible.
     *
     * @param head Should use `HEAD` instead of `GET` HTTP method to ping.
     */
    @JvmOverloads
    public suspend fun ping(head: Boolean = true): SystemPingData =
        requestCatching {
            if (head) {
                httpClient.head("/_ping")
            } else {
                httpClient.get("/_ping")
            }
        }.headers.let { headers ->
            SystemPingData(
                apiVersion = headers["API-Version"].orEmpty(),
                builderVersion = headers["Builder-Version"].orEmpty(),
                experimental = headers["Docker-Experimental"]?.toBooleanStrict() ?: false,
            )
        }

    /**
     * Monitors events in real-time from the server.
     *
     * @param options Options to filter the received events.
     */
    public fun events(options: MonitorEventsOptions = MonitorEventsOptions()): Flow<Event> =
        channelFlow {
            requestCatching {
                httpClient
                    .prepareGet("/events") {
                        parameter("until", options.until)
                        parameter("since", options.since)
                        parameter("filters", json.encodeToString(options.filters))
                    }.execute { response ->
                        val channel = response.body<ByteReadChannel>()
                        while (true) {
                            val raw = channel.readUTF8Line() ?: break
                            val decoded = json.decodeFromString<Event>(raw)
                            send(decoded)
                        }
                    }
            }
        }
}

/**
 * Monitors events in real-time from the server.
 *
 * @param options Options to filter the received events.
 */
public inline fun SystemResource.events(options: MonitorEventsOptions.() -> Unit): Flow<Event> =
    events(MonitorEventsOptions().apply(options))
