package me.devnatan.dockerkt

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.io.createHttpClient
import me.devnatan.dockerkt.resource.container.ContainerResource
import me.devnatan.dockerkt.resource.exec.ExecResource
import me.devnatan.dockerkt.resource.image.ImageResource
import me.devnatan.dockerkt.resource.network.NetworkResource
import me.devnatan.dockerkt.resource.secret.SecretResource
import me.devnatan.dockerkt.resource.system.SystemResource
import me.devnatan.dockerkt.resource.volume.VolumeResource
import me.devnatan.dockerkt.util.DockerKotlinJson
import kotlin.coroutines.CoroutineContext

public actual class DockerClient public actual constructor(
    public actual val config: DockerClientConfig,
) : CoroutineScope {
    public constructor() : this(DefaultDockerClientConfig)

    actual override val coroutineContext: CoroutineContext = SupervisorJob()

    public actual val json: Json get() = DockerKotlinJson
    public actual val httpClient: HttpClient = createHttpClient(this)

    @get:JvmName("images")
    public actual val images: ImageResource = ImageResource(httpClient, json)

    @get:JvmName("exec")
    public actual val exec: ExecResource = ExecResource(httpClient)

    @get:JvmName("containers")
    public actual val containers: ContainerResource = ContainerResource(this, json, httpClient, config)

    @get:JvmName("networks")
    public actual val networks: NetworkResource = NetworkResource(httpClient, json)

    @get:JvmName("volumes")
    public actual val volumes: VolumeResource = VolumeResource(httpClient, json)

    @get:JvmName("secrets")
    public actual val secrets: SecretResource = SecretResource(httpClient, json)

    @get:JvmName("system")
    public actual val system: SystemResource = SystemResource(httpClient, json)

    public actual fun close() {
        cancel()
        httpClient.close()
    }
}
