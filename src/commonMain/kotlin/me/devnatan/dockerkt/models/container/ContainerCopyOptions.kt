package me.devnatan.dockerkt.models.container

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class ContainerCopyOptions(
    /** Path inside the container where files will be copied to. */
    @SerialName("Path")
    var path: String,
    /**
     * If true, extract the tar archive in the destination directory.
     * If false, copy the tar archive itself.
     * Default: true
     */
    @Transient
    var extractArchive: Boolean = true,
    /**
     * If true, do not overwrite existing files/directories.
     * Default: false
     */
    @Transient
    var noOverwriteDirNonDir: Boolean = false,
    /**
     * If true, copy UID/GID maps for the files.
     * Default: false
     */
    @Transient
    var copyUIDGID: Boolean = false,
)
