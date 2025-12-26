package me.devnatan.dockerkt.models.exec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Options for creating a new exec instance in a running container.
 *
 * @property attachStdin Should attach standard input (`stdin`) on exec creation.
 * @property attachStdout Should attach standard output (`stdout`) on exec creation.
 * @property attachStderr Should attach standard error (stderr) on exec creation.
 * @property detachKeys Overrides the key sequence for detaching a container.
 *                      Accepted format is a single character `[a-Z]` or `ctrl-<value>`
 *                      where `value` is one of `a-z`, `@`, `^`, `[`, `,` or `_`.
 * @property tty Allocate a pseudo-TTY for exec instance.
 * @property env Set environment variables. Accepted format: `KEY=value`.
 * @property command The command to be run.
 * @property privileged Give extended privileges to the exec command.
 * @property user Username and/or group (optional) to run the exec process inside the container.
 *                Accepted formats: `username`, `username:group`, `uid` or `uid:gid`.
 * @property workingDir The working directory inside the container to run the command.
 */
@Serializable
public data class ExecCreateOptions(
    @SerialName("AttachStdin") var attachStdin: Boolean? = null,
    @SerialName("AttachStdout") var attachStdout: Boolean? = null,
    @SerialName("AttachStderr") var attachStderr: Boolean? = null,
    @SerialName("DetachKeys") var detachKeys: String? = null,
    @SerialName("Tty") var tty: Boolean? = null,
    @SerialName("Env") var env: List<String>? = null,
    @SerialName("Cmd") var command: List<String>? = null,
    @SerialName("Privileged") var privileged: Boolean? = null,
    @SerialName("User") var user: String? = null,
    @SerialName("WorkingDir") var workingDir: String? = null,
) {
    /**
     * Sets the command to be run.
     *
     * @param command Space-separated command to be run.
     */
    public fun setCommand(command: String?) {
        this.command = command?.split(" ")
    }
}
