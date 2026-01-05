package me.devnatan.dockerkt

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import me.devnatan.dockerkt.resource.container.ContainerResource
import me.devnatan.dockerkt.resource.exec.ExecResource
import me.devnatan.dockerkt.resource.image.ImageResource
import me.devnatan.dockerkt.resource.network.NetworkResource
import me.devnatan.dockerkt.resource.secret.SecretResource
import me.devnatan.dockerkt.resource.system.SystemResource
import me.devnatan.dockerkt.resource.volume.VolumeResource
import kotlin.coroutines.CoroutineContext

public expect class DockerClient(
    config: DockerClientConfig,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext

    public val config: DockerClientConfig
    public val json: Json
    public val httpClient: HttpClient
    public val images: ImageResource
    public val exec: ExecResource
    public val containers: ContainerResource
    public val networks: NetworkResource
    public val volumes: VolumeResource
    public val secrets: SecretResource
    public val system: SystemResource

    public fun close()
}
