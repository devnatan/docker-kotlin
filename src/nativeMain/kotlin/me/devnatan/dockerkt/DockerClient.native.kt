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
    public actual val config: DocketClientConfig,
) : CoroutineScope,
    Closeable {
    actual override val coroutineContext: CoroutineContext = SupervisorJob()

    public actual val json: Json get() = DockerKotlinJson
    public actual val httpClient: HttpClient = createHttpClient(this)

    public actual val images: ImageResource = ImageResource(httpClient, json)
    public actual val exec: ExecResource = ExecResource(httpClient)
    public actual val containers: ContainerResource = ContainerResource()
    public actual val networks: NetworkResource = NetworkResource(httpClient, json)
    public actual val volumes: VolumeResource = VolumeResource(httpClient, json)
    public actual val secrets: SecretResource = SecretResource(httpClient, json)
    public actual val system: SystemResource = SystemResource(httpClient, json)

    actual override fun close() {
        cancel()
    }
}
