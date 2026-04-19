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

public class DockerClient(
    public val config: DockerClientConfig = DefaultDockerClientConfig,
) : CoroutineScope,
    Closeable {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    public val json: Json get() = DockerKotlinJson
    public val httpClient: HttpClient = createHttpClient(this)

    public val images: ImageResource = ImageResource(httpClient, json)
    public val exec: ExecResource = ExecResource(httpClient)
    public val containers: ContainerResource = ContainerResource(httpClient, json)
    public val networks: NetworkResource = NetworkResource(httpClient, json)
    public val volumes: VolumeResource = VolumeResource(httpClient, json)
    public val secrets: SecretResource = SecretResource(httpClient, json)
    public val system: SystemResource = SystemResource(httpClient, json)

    override fun close() {
        cancel()
        httpClient.close()
    }
}
