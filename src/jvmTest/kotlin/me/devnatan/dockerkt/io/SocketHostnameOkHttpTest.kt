package me.devnatan.dockerkt.io

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SocketHostnameOkHttpTest {
    private fun buildOkHttpUrl(socketPath: String): HttpUrl {
        val hostname = encodeSocketPathHostname(socketPath)
        return "http://$hostname:$DockerSocketPort/v1.41/version".toHttpUrl()
    }

    @Test
    fun `OkHttp accepts encoded default socket path`() {
        val url = buildOkHttpUrl("/var/run/docker.sock")
        assertNotNull(url)
        assertEquals(DockerSocketPort, url.port)
    }

    @Test
    fun `OkHttp accepts encoded long socket path from issue 253`() {
        val url = buildOkHttpUrl("/Users/invoked/.docker/run/docker.sock")
        assertNotNull(url)
        assertEquals(DockerSocketPort, url.port)
    }

    @Test
    fun `OkHttp accepts encoded very long socket path`() {
        val url =
            buildOkHttpUrl(
                "/very/long/path/to/some/deeply/nested/docker/socket/directory/structure/docker.sock",
            )
        assertNotNull(url)
        assertEquals(DockerSocketPort, url.port)
    }

    @Test
    fun `OkHttp builds valid request with encoded hostname`() {
        val hostname = encodeSocketPathHostname("/Users/invoked/.docker/run/docker.sock")
        val url = "http://$hostname:$DockerSocketPort/v1.41/version".toHttpUrl()
        val request = Request.Builder().url(url).build()
        assertEquals("GET", request.method)
        assertEquals("/v1.41/version", request.url.encodedPath)
    }

    @Test
    fun `SocketDns resolves encoded hostname for unix socket`() {
        val hostname = encodeSocketPathHostname("/Users/invoked/.docker/run/docker.sock")
        val dns = SocketDns(isUnixSocket = true)
        val addresses = dns.lookup(hostname)
        assertEquals(1, addresses.size)
        assertEquals(hostname, addresses[0].hostName)
    }

    @Test
    fun `UnixSocketFactory decodes chunked hostname back to original path`() {
        val originalPath = "/Users/invoked/.docker/run/docker.sock"
        val hostname = encodeSocketPathHostname(originalPath)
        val decoded = decodeSocketPathHostname(hostname)
        assertEquals(originalPath, decoded)
    }

    @Test
    fun `OkHttpClient can be configured with SocketDns and encoded hostname`() {
        val hostname = encodeSocketPathHostname("/Users/invoked/.docker/run/docker.sock")
        val client =
            OkHttpClient
                .Builder()
                .dns(SocketDns(isUnixSocket = true))
                .build()

        val url = "http://$hostname:$DockerSocketPort/v1.41/version".toHttpUrl()
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        assertNotNull(call)
    }

    @Test
    fun `encoded hostname roundtrips through OkHttp URL parsing`() {
        val paths =
            listOf(
                "/var/run/docker.sock",
                "/Users/invoked/.docker/run/docker.sock",
                "/home/user/.local/share/docker/run/docker.sock",
                "/very/long/path/to/some/deeply/nested/docker/socket/directory/structure/docker.sock",
            )

        for (path in paths) {
            val hostname = encodeSocketPathHostname(path)
            val url = "http://$hostname:$DockerSocketPort/v1.41/version".toHttpUrl()
            val parsedHost = url.host
            val decoded = decodeSocketPathHostname(parsedHost)
            assertEquals(path, decoded, "Roundtrip failed for path: $path")
        }
    }
}
