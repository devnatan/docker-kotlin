package me.devnatan.dockerkt.native

public expect class CustomDnsResolver {

    public suspend fun resolve(hostname: String, dnsServers: List<String> = emptyList()): Result<List<String>>
}