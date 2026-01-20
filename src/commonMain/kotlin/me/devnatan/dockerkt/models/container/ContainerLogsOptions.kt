@file:OptIn(ExperimentalTime::class)

package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmOverloads
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Container logs endpoint options.
 *
 * @property follow Should keep connection after returning logs.
 * @property stdout Returns logs from `stdout`.
 * @property stderr Return logs from `stderr`.
 * @property since Only return logs since this time, as a UNIX timestamp.
 * @property until Only return logs before this time, as a UNIX timestamp.
 * @property showTimestamps Should add timestamps to every log line.
 * @property tail Only return this number of log lines from the end of the logs. Set to `null` to output all log lines.
 * @property splitLineBreaks Should split lines separated by line break into multiple frames.
 */
@Serializable
public class ContainerLogsOptions
    @JvmOverloads
    constructor(
        public var follow: Boolean? = null,
        public var stdout: Boolean? = null,
        public var stderr: Boolean? = null,
        public var since: Long? = null,
        public var until: Long? = null,
        @SerialName("timestamps") public var showTimestamps: Boolean? = null,
        public var tail: String? = null,
        public var splitLineBreaks: Boolean = false,
        /**
         * Whether to separate stdout and stderr streams.
         * Note: For TTY-enabled containers, all output is treated as stdout.
         */
        @Transient public var demux: Boolean = false,
    ) {
        public fun setTailAll() {
            this.tail = "all"
        }

        public fun setTail(size: Int) {
            this.tail = size.toString()
        }

        /**
         * Only return logs since this time, as a UNIX timestamp.
         * @param since The timestamp.
         */
        public fun ContainerLogsOptions.setSince(since: Instant?) {
            this.since = since?.epochSeconds
        }

        /**
         * Only return logs before this time, as a UNIX timestamp.
         * @param until The timestamp.
         */
        public fun ContainerLogsOptions.setUntil(until: Instant?) {
            this.until = until?.epochSeconds
        }
    }
