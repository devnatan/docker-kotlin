package me.devnatan.dockerkt.resource.secret

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.IdOnlyResponse
import me.devnatan.dockerkt.models.secret.Secret
import me.devnatan.dockerkt.models.secret.SecretListFilters
import me.devnatan.dockerkt.models.secret.SecretSpec
import me.devnatan.dockerkt.resource.swarm.NodeNotPartOfSwarmException

/**
 * Secrets are sensitive data that can be used by Docker services.
 * Swarm mode must be enabled for these endpoints to work.
 */
public class SecretResource internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private companion object {
        const val BASE_PATH = "/secrets"
    }

    /**
     * Lists all secrets.
     * @param filters Filters to process on the secrets list.
     */
    public suspend fun list(filters: SecretListFilters? = null): List<Secret> =
        requestCatching(
            HttpStatusCode.ServiceUnavailable to ::NodeNotPartOfSwarmException,
        ) {
            httpClient.get(BASE_PATH) {
                parameter("filters", filters?.let(json::encodeToString))
            }
        }.body()

    /**
     * Inspects a secret.
     *
     * @param id The id of the secret.
     */
    public suspend fun inspect(id: String): Secret = httpClient.get("$BASE_PATH/$id").body()

    /**
     * Deletes a secret.
     *
     * @param id The id of the secret.
     */
    public suspend fun delete(id: String) {
        httpClient.delete("$BASE_PATH/$id")
    }

    /**
     * Creates a new secret on the Docker swarm.
     *
     * @param options The secret spec.
     */
    public suspend fun create(options: SecretSpec): String =
        requestCatching(
            HttpStatusCode.Conflict to { SecretNameConflictException(it, options.name) },
            HttpStatusCode.ServiceUnavailable to ::NodeNotPartOfSwarmException,
        ) {
            httpClient.post("$BASE_PATH/create") {
                setBody(options)
            }
        }.body<IdOnlyResponse>().id

    /**
     * Updates an existing secret on the Docker swarm.
     *
     * @param id The secret id.
     * @param version The secret version.
     * @param options The new secret spec.
     */
    public suspend fun update(
        id: String,
        version: Long,
        options: SecretSpec,
    ) {
        requestCatching(
            HttpStatusCode.NotFound to { SecretNotFoundException(it, id, version) },
            HttpStatusCode.ServiceUnavailable to ::NodeNotPartOfSwarmException,
        ) {
            httpClient.post("$BASE_PATH/$id/update") {
                parameter("version", version)
                setBody(options)
            }
        }
    }
}

/**
 * Lists all secrets.
 *
 * @param filters Filters to process on the secrets list.
 */
public suspend inline fun SecretResource.list(filters: SecretListFilters.() -> Unit): List<Secret> =
    list(SecretListFilters().apply(filters))

public suspend inline fun SecretResource.create(options: SecretSpec.() -> Unit): String = create(SecretSpec().apply(options))

public suspend inline fun SecretResource.update(
    id: String,
    version: Long,
    options: SecretSpec.() -> Unit,
): Unit = update(id, version, SecretSpec().apply(options))

/**
 * Sets the secret list filters to the given label pair.
 *
 * @param label The label pair (key, value).
 */
public fun SecretListFilters.label(label: Pair<String, String>) {
    this.label = "${label.first}=${label.second}"
}
