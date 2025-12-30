package me.devnatan.dockerkt.models.container

public data class ContainerCopyResult(
    val archiveData: ByteArray,
    val stat: ContainerArchiveInfo?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ContainerCopyResult

        if (!archiveData.contentEquals(other.archiveData)) return false
        if (stat != other.stat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = archiveData.contentHashCode()
        result = 31 * result + (stat?.hashCode() ?: 0)
        return result
    }
}
