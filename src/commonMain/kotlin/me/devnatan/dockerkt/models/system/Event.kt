@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
public data class Event internal constructor(
    @SerialName("Type") public val type: EventType,
    @SerialName("Action") private val rawAction: String,
    @SerialName("Actor") public val actor: EventActor,
    @SerialName("scope") public val scope: EventScope,
    @SerialName("time") public val timeMillis: Long,
    @SerialName("timeNano") public val timeNanos: Long,
) {
    val result: String? by lazy {
        rawAction
            .substringAfter(
                delimiter = ":",
                missingDelimiterValue = "",
            ).ifEmpty { null }
            ?.trim()
    }

    val action: EventAction by lazy {
        EventAction.entries.first { entry ->
            entry.name.equals(rawAction.substringBefore(":").uppercase(), ignoreCase = true)
        }
    }
}

public val Event.time: Instant get() = Instant.fromEpochMilliseconds(timeMillis)

@Serializable
public enum class EventAction {
    @SerialName("attach")
    Attach,

    @SerialName("commit")
    Commit,

    @SerialName("copy")
    Copy,

    @SerialName("create")
    Create,

    @SerialName("delete")
    Delete,

    @SerialName("destroy")
    Destroy,

    @SerialName("detach")
    Detach,

    @SerialName("die")
    Die,

    @SerialName("exec_create")
    ExecCreate,

    @SerialName("exec_detach")
    ExecDetach,

    @SerialName("exec_start")
    ExecStart,

    @SerialName("exec_die")
    ExecDie,

    @SerialName("import")
    Import,

    @SerialName("export")
    Export,

    @SerialName("health_status")
    Health,

    @SerialName("kill")
    Kill,

    @SerialName("oom")
    Oom,

    @SerialName("pause")
    Pause,

    @SerialName("rename")
    Rename,

    @SerialName("resize")
    Resize,

    @SerialName("restart")
    Restart,

    @SerialName("start")
    Start,

    @SerialName("stop")
    Stop,

    @SerialName("top")
    Top,

    @SerialName("unpause")
    Unpause,

    @SerialName("update")
    Update,

    @SerialName("prune")
    Prune,

    @SerialName("remove")
    Remove,

    @SerialName("load")
    ImageLoad,

    @SerialName("pull")
    ImagePull,

    @SerialName("push")
    ImagePush,

    @SerialName("save")
    ImageSave,

    @SerialName("tag")
    ImageTag,

    @SerialName("untag")
    ImageUntag,

    @SerialName("reload")
    DaemonReload,

    @SerialName("connect")
    NetworkConnect,

    @SerialName("disconnect")
    NetworkDisconnect,
    Unknown,
}

@Serializable
public enum class EventType {
    @SerialName("builder")
    Builder,

    @SerialName("config")
    Config,

    @SerialName("container")
    Container,

    @SerialName("daemon")
    Daemon,

    @SerialName("image")
    Image,

    @SerialName("network")
    Network,

    @SerialName("node")
    Node,

    @SerialName("plugin")
    Plugin,

    @SerialName("secret")
    Secret,

    @SerialName("service")
    Service,

    @SerialName("volume")
    Volume,
    Unknown,
}

@Serializable
public data class EventActor internal constructor(
    @SerialName("ID") public val id: String,
    @SerialName("Attributes") public val attributes: Map<String, String> = emptyMap(),
)

@Serializable
public enum class EventScope {
    @SerialName("local")
    Local,

    @SerialName("swarm")
    Swarm,

    Unknown,
}
