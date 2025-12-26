package me.devnatan.dockerkt.models.image

import me.devnatan.dockerkt.models.RegistryConfig

/**
 * Options for building a Docker image.
 *
 * @property path The path to the build context.
 * @property tag The tag to be applied to the built image.
 * @property extraHosts Additional hosts to add to /etc/hosts in the build containers.
 * @property remote URL of the Docker daemon to connect to.
 * @property suppressVerbose Suppress verbose output during build.
 * @property noCache Do not use the cache when building the image.
 * @property cacheFrom List of images used for cache resolution.
 * @property pull Always attempt to pull a newer version of the image.
 * @property removeIntermediateContainers Remove intermediate containers after a successful build.
 * @property forceRemoveIntermediateContainers Force removal of intermediate containers.
 * @property memoryLimit Memory limit for the build container.
 * @property memorySwap Swap limit equal to memory plus swap.
 * @property cpuShares CPU shares for the build container.
 * @property cpuSetCpus CPUs in which to allow execution.
 * @property cpuPeriod CPU period to be used for the build container.
 * @property cpuQuota CPU quota to be used for the build container.
 * @property buildArgs Build-time variables to set during the build.
 * @property shmSize Size of /dev/shm in bytes.
 * @property squash Squash newly built layers into a single new layer.
 * @property labels User-defined key-value metadata to add to the image.
 * @property networkMode Network mode for the build container.
 * @property platform Platform in the format os[/arch[/variant]].
 * @property target Build only a specific target stage.
 * @property outputs Specify the outputs to be built.
 * @property version Version of the build options.
 * @property registryConfig Docker registry auth configurations for multiple registries that a build may refer to.
 */
public data class ImageBuildOptions(
    public var path: String? = null,
    public var tag: String? = null,
    public var extraHosts: String? = null,
    public var remote: String? = null,
    public var suppressVerbose: Boolean? = null,
    public var noCache: Boolean? = null,
    public var cacheFrom: List<String> = emptyList(),
    public var pull: String? = null,
    public var removeIntermediateContainers: Boolean? = null,
    public var forceRemoveIntermediateContainers: Boolean? = null,
    public var memoryLimit: Int? = null,
    public var memorySwap: Int? = null,
    public var cpuShares: Int? = null,
    public var cpuSetCpus: String? = null,
    public var cpuPeriod: Int? = null,
    public var cpuQuota: Int? = null,
    public var buildArgs: Map<String, String> = emptyMap(),
    public var shmSize: Int? = null,
    public var squash: Boolean? = null,
    public var labels: Map<String, String> = emptyMap(),
    public var networkMode: String? = null,
    public var platform: String? = null,
    public var target: String? = null,
    public var outputs: String? = null,
    public var version: String? = null,
    public var registryConfig: Map<String, RegistryConfig> = emptyMap(),
)
