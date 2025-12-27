package me.devnatan.dockerkt.models

public open class Resource internal constructor() {
    internal var _rawValues: Map<String, String> = emptyMap()

    public val rawValues: Map<String, String>
        get() = _rawValues
}
