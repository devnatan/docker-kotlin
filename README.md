# docker-kotlin

docker-kotlin allows you to interact with the Docker Engine Remote API.

## Installation

```kotlin
dependencies {
    implementation("me.devnatan:docker-kotlin:0.10.0")
}
```

## Basic Usage

Use `DockerKotlin.create()` to create a new Docker client instance with the default settings, default settings are based on the
current platform or environment variables, e.g.: socket path will be set to [`DOCKER_HOST`](https://docs.docker.com/compose/environment-variables/envvars/#docker_host)
if present otherwise `unix://var/run/docker.sock` if the current platform is Unix-like.

```kotlin
val client = DockerClient.create()
```

To change the default configuration properties use `DockerClientConfig` and `DockerClient` overload.

```kotlin
val client = DockerClient {
    // this: DockerClientConfigBuilder
}
```


## Resources

* [System](#system)
* [Containers](#containers)
* [Networks](#networks)
* [Exec](#exec)

### System

#### Get System Information

```kotlin
val version: SystemVersion = client.system.version()
```

### Containers

#### Create and start a Container with explicit port bindings

```kotlin
val containerId = client.containers.create("busybox:latest") {
    // Only if your container doesn't already expose this port
    exposedPort(80u)

    hostConfig {
        portBindings(80u) {
            add(PortBinding("0.0.0.0", 8080u))
        }
    }
}

client.containers.start(containerId)
```

#### Create and start a Container with auto-assigned port bindings

```kotlin
val containerId = client.containers.create("busybox:latest") {
    // Only if your container doesn't already expose this port
    exposedPort(80u)
    
    hostConfig {
        portBindings(80u)
    }
}

client.containers.start(containerId)

// Inspect the container to retrieve the auto-assigned ports
val container = testClient.containers.inspect(id)
val ports = container.networkSettings.ports
```

#### List All Containers

```kotlin
val containers: List<Container> = client.containers.list()
```

#### Stream Container Logs

```kotlin
val logs: Flow<Frame> = client.containers.logs("floral-fury") {
    stderr = true
    stdout = true
}

logs.onStart { /* streaming started */ }
    .onCompletion { /* streaming finished */ }
    .catch { /* something went wrong */ }
    .collect { log -> /* do something with each log */ }
```

### Networks

#### Create a new Network

```kotlin
val networkId: String = client.networks.create {
    name = "octopus-net"
    driver = "overlay"
}
```

#### List all Networks
```kotlin
val networks = client.networks.list()
```

#### Connect a container to a network
```kotlin
client.networks.connect(networkId, containerId)
```

### Exec

#### Execute a command in a running container
```kotlin
val execId = client.exec.create(containerId) {
    command = listOf("echo", "Hello, Docker!")
    attachStdout = true
}

val result = client.exec.start(execId, ExecStartOptions())
when (result) {
    is ExecStartResult.Complete -> println(result.output)
    else -> error("Unexpected result")
}
```

#### Execute a command with streaming output
```kotlin
val execId = client.exec.create(containerId) {
    command = listOf("sh", "-c", "for i in 1 2 3; do echo line \$i; sleep 1; done")
    attachStdout = true
}

val result = client.exec.start(execId) { stream = true }
when (result) {
    is ExecStartResult.Stream -> {
        result.output.collect { chunk ->
            print(chunk)
        }
    }
    else -> error("Unexpected result")
}
```

#### Execute a command with separated stdout/stderr
```kotlin
val execId = client.exec.create(containerId) {
    command = listOf("sh", "-c", "echo stdout; echo stderr >&2")
    attachStdout = true
    attachStderr = true
}

val result = client.exec.start(execId) { demux = true }
when (result) {
    is ExecStartResult.CompleteDemuxed -> {
        println("STDOUT: ${result.output.stdout}")
        println("STDERR: ${result.output.stderr}")
    }
    else -> error("Unexpected result")
}
```

#### Check exec exit code
```kotlin
val execId = client.exec.create(containerId) {
    command = listOf("false")
}

client.exec.start(execId) { detach = true }

val execInfo = client.exec.inspect(execId)
println("Exit code: ${execInfo.exitCode}") // Exit code: 1
```

### File Operations

#### Copy a file from container to host
```kotlin
client.containers.copyFileFrom(
    containerId,
    sourcePath = "/var/log/app.log",
    destinationPath = "/tmp/app.log"
)
```

##### Copy a file from host to container
```kotlin
client.containers.copyFileTo(
    containerId,
    sourcePath = "/home/user/config.json",
    destinationPath = "/app/config/"
)
```

#### Copy a directory from container to host
```kotlin
client.containers.copyDirectoryFrom(
    containerId,
    sourcePath = "/app/logs",
    destinationPath = "/tmp/container-logs"
)
```

#### Copy a directory from host to container
```kotlin
client.containers.copyDirectoryTo(
    containerId,
    sourcePath = "/home/user/configs",
    destinationPath = "/app/"
)
```

#### Advanced copy with custom options
```kotlin
// Copy with custom options
client.containers.copy.copyTo(
    container = containerId,
    destinationPath = "/app/data",
    tarArchive = myTarArchive
) {
    path = "/app/data"
    noOverwriteDirNonDir = true  // Don't overwrite if types mismatch
    copyUIDGID = true            // Preserve UID/GID
}

// Get raw tar archive from container
val result = client.containers.copyFrom(containerId, "/app/config")
val tarData = result.archiveData

// Archive info including file metadata
val stats = result.stat 
```

## License

docker-kotlin is licensed under the MIT license.
