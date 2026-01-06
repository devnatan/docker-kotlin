package me.devnatan.dockerkt.io

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.DockerClient
import me.devnatan.dockerkt.DockerResponseException
import me.devnatan.dockerkt.GenericDockerErrorResponse

internal expect fun createHttpClient(dockerClient: DockerClient): HttpClient

internal fun HttpClientConfig<*>.configureBaseHttpClient(dockerClient: DockerClient) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }

    if (dockerClient.config.debugHttpCalls) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }

    install(UserAgent) { agent = "docker-kotlin" }

    HttpResponseValidator {
        handleResponseExceptionWithRequest { exception, _ ->
            val responseException = exception as? ResponseException ?: return@handleResponseExceptionWithRequest
            val exceptionResponse = responseException.response

            val errorMessage =
                runCatching {
                    exceptionResponse.body<GenericDockerErrorResponse>()
                }.getOrNull()?.message
            throw DockerResponseException(
                cause = exception,
                message = errorMessage,
                statusCode = exceptionResponse.status,
            )
        }
    }

    defaultRequest {
        contentType(ContentType.Application.Json)

        // workaround for URL prepending
        // https://github.com/ktorio/ktor/issues/537#issuecomment-603272476
        url.takeFrom(
            URLBuilder(createUrlBuilder(dockerClient.config.socketPath)).apply {
                encodedPath = "/v${dockerClient.config.apiVersion}/"
                encodedPath += url.encodedPath
            },
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun createUrlBuilder(socketPath: String): URLBuilder =
    if (isUnixSocket(socketPath)) {
        URLBuilder(
            protocol = URLProtocol.HTTP,
            port = DockerSocketPort,
            host = socketPath.substringAfter(UnixSocketPrefix).encodeToByteArray().toHexString() + EncodedHostnameSuffix,
        )
    } else {
        val url = Url(socketPath)
        URLBuilder(
            protocol = URLProtocol.HTTP,
            host = url.host,
            port = url.port,
        )
    }

internal fun handleHttpFailure(
    exception: Throwable,
    statuses: Map<HttpStatusCode, (DockerResponseException) -> Throwable>,
) {
    if (exception !is DockerResponseException) {
        throw exception
    }

    val resourceException =
        statuses.entries
            .firstOrNull { (code, _) ->
                code == exception.statusCode
            }?.value

    throw resourceException
        ?.invoke(exception)
        ?.also { root -> root.addSuppressed(exception) }
        ?: exception
}

// TODO use Ktor exception handler instead
internal inline fun <T> requestCatching(
    vararg errors: Pair<HttpStatusCode, (DockerResponseException) -> Throwable>,
    request: () -> T,
) = runCatching(request).onFailure { exception -> handleHttpFailure(exception, errors.toMap()) }.getOrThrow()
