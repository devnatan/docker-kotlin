package me.devnatan.dockerkt.resource.image

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.io.requestCatching
import me.devnatan.dockerkt.models.image.Image
import me.devnatan.dockerkt.models.image.ImageBuildOptions
import me.devnatan.dockerkt.models.image.ImagePull
import me.devnatan.dockerkt.models.image.ImageSummary

private const val BasePath = "/images"

public class ImageResource internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    public companion object {
        private val TAR_CONTENT_TYPE = ContentType.parse("application/x-tar")
    }

    public suspend fun list(): List<ImageSummary> = httpClient.get("$BasePath/json").body()

    public fun pull(image: String): Flow<ImagePull> =
        channelFlow {
            httpClient
                .preparePost("$BasePath/create") {
                    parameter("fromImage", image)
                }.execute { response ->
                    val channel = response.body<ByteReadChannel>()
                    while (true) {
                        val line = channel.readUTF8Line() ?: break
                        send(json.decodeFromString(line))
                    }
                }
        }

    public suspend fun remove(
        name: String,
        force: Boolean? = false,
        noprune: Boolean? = false,
    ) {
        requestCatching(
            HttpStatusCode.NotFound to { exception -> ImageNotFoundException(exception, name) },
        ) {
            httpClient.delete("$BasePath/$name") {
                parameter("force", force)
                parameter("noprune", noprune)
            }
        }
    }

    /**
     * builds a Docker image using the specified archive path and [ImageBuildOptions].
     *
     * @param archivePath The path to the build context archive (e.g., a TAR file) that contains the source code and resources.
     * @param options The [ImageBuildOptions] containing the configuration for the image build.
     */
    public suspend fun build(
        archivePath: String,
        options: ImageBuildOptions,
    ) {
        httpClient.post("/build") {
            contentType(TAR_CONTENT_TYPE)
            header("X-Registry-Config", json.encodeToString(options.registryConfig))
            parameter("dockerfile", options.path)
            parameter("t", options.tag)
            parameter("extrahosts", options.extraHosts)
            parameter("remote", options.remote)
            parameter("q", options.suppressVerbose)
            parameter("nocache", options.noCache)
            parameter("cachefrom", options.cacheFrom)
            parameter("pull", options.pull)
            parameter("rm", options.removeIntermediateContainers)
            parameter("forcerm", options.forceRemoveIntermediateContainers)
            parameter("memory", options.memoryLimit)
            parameter("memswap", options.memorySwap)
            parameter("cpushares", options.cpuShares)
            parameter("cpusetcpus", options.cpuSetCpus)
            parameter("cpuperiod", options.cpuPeriod)
            parameter("cpuquota", options.cpuQuota)
            parameter("buildargs", options.buildArgs)
            parameter("shmsize", options.shmSize)
            parameter("squash", options.squash)
            parameter("labels", options.labels)
            parameter("networkmode", options.networkMode)
            parameter("platform", options.platform)
            parameter("target", options.target)
            parameter("outputs", options.outputs)
            parameter("version", options.version)
            setBody(archivePath)
        }
    }

    /**
     * Return low-level information about an image.
     *
     * @param image Image name or ID.
     * @throws ImageNotFoundException If the image is not found.
     */
    public suspend fun inspect(image: String): Image =
        requestCatching(
            HttpStatusCode.NotFound to { exception ->
                ImageNotFoundException(exception, image)
            },
        ) {
            httpClient.get("$BasePath/$image/json")
        }.body()
}

public suspend inline fun ImageResource.build(
    archivePath: String,
    options: ImageBuildOptions.() -> Unit,
) {
    build(archivePath, ImageBuildOptions().apply(options))
}
