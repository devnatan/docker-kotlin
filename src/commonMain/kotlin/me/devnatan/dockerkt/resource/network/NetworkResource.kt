package me.devnatan.dockerkt.resource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.IdOnlyResponse
import me.devnatan.dockerkt.models.network.Network
import me.devnatan.dockerkt.models.network.NetworkCreateOptions
import me.devnatan.dockerkt.models.network.NetworkInspectOptions
import me.devnatan.dockerkt.models.network.NetworkListFilters
import me.devnatan.dockerkt.models.network.NetworkPruneOptions
import me.devnatan.dockerkt.models.network.NetworkPruneResult

private const val BasePath = "/networks"

/**
 * Networks are user-defined networks that containers can be attached to.
 * See the [networking documentation](https://docs.docker.com/network/) for more information.
 */
public class NetworkResource internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    /**
     * Returns a list of networks.
     *
     * @param filters Filters to process on the networks list.
     * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkList">NetworkList</a>
     */
    public suspend fun list(filters: NetworkListFilters? = null): List<Network> =
        httpClient
            .get(BasePath) {
                parameter("filters", filters?.let(json::encodeToString))
            }.body()

    /**
     * Inspects a network.
     *
     * @param network The network id or name.
     * @param options The network inspection options.
     * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkInspect">NetworkInspect</a>
     */
    public suspend fun inspect(
        network: String,
        options: NetworkInspectOptions? = null,
    ): Network =
        requestCatching(
            HttpStatusCode.NotFound to { NetworkNotFoundException(it, network) },
        ) {
            httpClient.get("$BasePath/$network") {
                parameter("verbose", options?.verbose)
                parameter("scope", options?.scope)
            }
        }.body()

    /**
     * Removes a network.
     *
     * @param network The network id or name.
     * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkDelete">NetworkDelete</a>
     */
    public suspend fun remove(network: String) {
        httpClient.delete("$BasePath/$network")
    }

    /**
     * Creates a new network.
     *
     * @param config The network configuration.
     * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkCreate">NetworkCreate</a>
     */
    public suspend fun create(config: NetworkCreateOptions): String {
        checkNotNull(config.name) { "Network name is required and cannot be null" }

        return requestCatching(
            HttpStatusCode.Conflict to { exception ->
                NetworkConflictException(exception, config.name!!)
            },
            HttpStatusCode.Forbidden to { exception ->
                NetworkForbiddenException(exception, config.name!!)
            },
        ) {
            httpClient
                .post("$BasePath/create") {
                    setBody(config)
                }.body<IdOnlyResponse>()
                .id
        }
    }

    /**
     * Deletes all unused networks.
     *
     * Networks are considered unused if they have no containers attached to them.
     *
     * @param options The network prune options. Use [prune] extension function for DSL syntax.
     * @return Information about the pruned networks.
     */
    public suspend fun prune(options: NetworkPruneOptions? = null): NetworkPruneResult =
        httpClient
            .post("$BasePath/prune") {
                options?.let {
                    parameter("filters", json.encodeToString(it))
                }
            }.body()

    /**
     * Connects a container to a network.
     *
     * @param network The network id or name.
     * @param container The id or name of the container to connect to the network.
     * @see <a href="https://docs.docker.com/engine/api/v1.41/#operation/NetworkConnect">NetworkConnect</a>
     */
    public suspend fun connectContainer(
        network: String,
        container: String,
    ) {
        httpClient.post("$BasePath/$network/connect") {
            setBody(mapOf("Container" to container))
        }
    }

    /**
     * Disconnects a container to a network.
     *
     * @param network The network id or name.
     * @param container The id or name of the container to connect to the network.
     * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkDisconnect">NetworkDisconnect</a>
     */
    public suspend fun disconnectContainer(
        network: String,
        container: String,
    ) {
        httpClient.post("$BasePath/$network/disconnect") {
            setBody(mapOf("Container" to container))
        }
    }
}

/**
 * Creates a new network.
 *
 * @param config The network configuration.
 * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkCreate">NetworkCreate</a>
 */
public suspend inline fun NetworkResource.create(config: NetworkCreateOptions.() -> Unit): String =
    create(NetworkCreateOptions().apply(config))

/**
 * Returns a list of networks.
 *
 * @param filters Filters to process on the networks list.
 * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkList">NetworkList</a>
 */
public suspend inline fun NetworkResource.list(filters: NetworkListFilters.() -> Unit): List<Network> =
    list(NetworkListFilters().apply(filters))

/**
 * Inspects a network.
 *
 * @param id The network id or name.
 * @param options The network inspection options.
 * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkInspect">NetworkInspect</a>
 */
public suspend inline fun NetworkResource.inspect(
    id: String,
    options: NetworkInspectOptions.() -> Unit,
): Network = inspect(id, NetworkInspectOptions().apply(options))

/**
 * Deletes all unused networks.
 *
 * @param options The prune options configuration block.
 * @return Information about the pruned networks.
 *
 * @see <a href="https://docs.docker.com/engine/api/latest/#operation/NetworkPrune"
 */
public suspend inline fun NetworkResource.prune(options: NetworkPruneOptions.() -> Unit): NetworkPruneResult =
    prune(NetworkPruneOptions().apply(options))
