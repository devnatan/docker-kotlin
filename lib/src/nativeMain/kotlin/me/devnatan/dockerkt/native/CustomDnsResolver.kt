@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
package me.devnatan.dockerkt.native

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.devnatan.dockerkt.interop.dns_resolve
import me.devnatan.dockerkt.interop.dns_result_free
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
public actual class CustomDnsResolver {
    public actual suspend fun resolve(
        hostname: String,
        dnsServers: List<String>
    ): Result<List<String>> = withContext(Dispatchers.Default) {
        memScoped {
            val dnsServersArray = if (dnsServers.isNotEmpty()) {
                allocArrayOf(dnsServers.map { it.cstr.ptr })
            } else {
                null
            }

            val result = dns_resolve(
                hostname,
                dnsServersArray,
                dnsServers.size.toULong()
            ) ?: return@withContext Result.failure(Exception("DNS resolution returned null"))

            try {
                val dnsResult = result.pointed

                if (dnsResult.error != null) {
                    val errorMsg = dnsResult.error!!.toKString()
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val addresses = mutableListOf<String>()
                for (i in 0 until dnsResult.count.toInt()) {
                    val addrPtr = dnsResult.addresses!![i]
                    if (addrPtr != null) {
                        addresses.add(addrPtr.toKString())
                    }
                }

                Result.success(addresses)
            } finally {
                dns_result_free(result)
            }
        }
    }
}
