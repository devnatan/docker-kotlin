@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class Event internal constructor(
    @SerialName("Type") public val type: EventType,
    @SerialName("Action") public val action: EventAction,
    @SerialName("Actor") public val actor: EventActor,
    @SerialName("scope") public val scope: EventScope,
    @SerialName("time") public val timeMillis: Long,
    @SerialName("timeNano") public val timeNanos: Long,
)

public val Event.time: Instant get() = Instant.fromEpochMilliseconds(timeMillis)

@Serializable
public enum class EventAction {
    @SerialName("attach")
    ATTACH,

    @SerialName("commit")
    COMMIT,

    @SerialName("copy")
    COPY,

    @SerialName("create")
    CREATE,

    @SerialName("delete")
    DELETE,

    @SerialName("destroy")
    DESTROY,

    @SerialName("detach")
    DETACH,

    @SerialName("die")
    DIE,

    @SerialName("exec_create")
    EXEC_CREATE,

    @SerialName("exec_detach")
    EXEC_DETACH,

    @SerialName("exec_start")
    EXEC_START,

    @SerialName("exec_die")
    EXEC_DIE,

    @SerialName("import")
    IMPORT,

    @SerialName("export")
    EXPORT,

    @SerialName("health_status")
    HEALTH,

    @SerialName("kill")
    KILL,

    @SerialName("oom")
    OOM,

    @SerialName("pause")
    PAUSE,

    @SerialName("rename")
    RENAME,

    @SerialName("resize")
    RESIZE,

    @SerialName("restart")
    RESTART,

    @SerialName("start")
    START,

    @SerialName("stop")
    STOP,

    @SerialName("top")
    TOP,

    @SerialName("unpause")
    UNPAUSE,

    @SerialName("update")
    UPDATE,

    @SerialName("prune")
    PRUNE,

    @SerialName("remove")
    REMOVE,

    @SerialName("load")
    IMAGE_LOAD,

    @SerialName("pull")
    IMAGE_PULL,

    @SerialName("push")
    IMAGE_PUSH,

    @SerialName("save")
    IMAGE_SAVE,

    @SerialName("tag")
    IMAGE_TAG,

    @SerialName("untag")
    IMAGE_UNTAG,

    @SerialName("reload")
    DAEMON_RELOAD,

    @SerialName("connect")
    NET_CONNECT,

    @SerialName("disconnect")
    NET_DISCONNECT,
    UNKNOWN,
}

@Serializable
public enum class EventType {
    @SerialName("builder")
    BUILDER,

    @SerialName("config")
    CONFIG,

    @SerialName("container")
    CONTAINER,

    @SerialName("daemon")
    DAEMON,

    @SerialName("image")
    IMAGE,

    @SerialName("network")
    NETWORK,

    @SerialName("node")
    NODE,

    @SerialName("plugin")
    PLUGIN,

    @SerialName("secret")
    SECRET,

    @SerialName("service")
    SERVICE,

    @SerialName("volume")
    VOLUME,
    UNKNOWN,
}

@Serializable
public data class EventActor internal constructor(
    @SerialName("ID") public val id: String,
    @SerialName("Attributes") public val attributes: Map<String, String> = emptyMap(),
)

@Serializable
public enum class EventScope {
    @SerialName("local")
    LOCAL,

    @SerialName("swarm")
    SWARM,
    UNKNOWN,
}
