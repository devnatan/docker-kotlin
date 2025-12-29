package me.devnatan.dockerkt.io

import okhttp3.Dns
import org.newsclub.net.unix.AFUNIXSocketAddress
import org.newsclub.net.unix.AFUNIXSocketFactory
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Paths

internal class SocketDns(
    private val isUnixSocket: Boolean,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        if (isUnixSocket) {
            listOf(
                InetAddress.getByAddress(
                    hostname,
                    byteArrayOf(0, 0, 0, 0),
                ),
            )
        } else {
            Dns.SYSTEM.lookup(hostname)
        }
}

internal class UnixSocketFactory : AFUNIXSocketFactory() {
    @OptIn(ExperimentalStdlibApi::class)
    private fun decodeHostname(hostname: String): String =
        hostname
            .substring(0, hostname.indexOf(EncodedHostnameSuffix))
            .hexToByteArray()
            .decodeToString()

    override fun addressFromHost(
        host: String,
        port: Int,
    ): AFUNIXSocketAddress {
        val socketPath = decodeHostname(host)
        val socketFile = Paths.get(socketPath) ?: error("Unable to connect to unix socket @ $socketPath")

        if (!Files.exists(socketFile) || !Files.isWritable(socketFile)) {
            error("Socket path do not exists or is not writable: $socketFile")
        }

        return AFUNIXSocketAddress.of(socketFile)
    }
}
