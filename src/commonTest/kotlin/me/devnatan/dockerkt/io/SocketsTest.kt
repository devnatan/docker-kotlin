package me.devnatan.dockerkt.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SocketsTest {
    @Test
    fun `encode and decode short socket path`() {
        val path = "/var/run/docker.sock"
        val encoded = encodeSocketPathHostname(path)
        val decoded = decodeSocketPathHostname(encoded)
        assertEquals(path, decoded)
    }

    @Test
    fun `encode and decode long socket path`() {
        val path = "/Users/invoked/.docker/run/docker.sock"
        val encoded = encodeSocketPathHostname(path)
        val decoded = decodeSocketPathHostname(encoded)
        assertEquals(path, decoded)
    }

    @Test
    fun `encoded hostname ends with socket suffix`() {
        val encoded = encodeSocketPathHostname("/var/run/docker.sock")
        assertTrue(encoded.endsWith(EncodedHostnameSuffix))
    }

    @Test
    fun `all dns labels are within max length`() {
        val path = "/Users/invoked/.docker/run/docker.sock"
        val encoded = encodeSocketPathHostname(path)
        val labels = encoded.removeSuffix(EncodedHostnameSuffix).split(".")
        for (label in labels) {
            assertTrue(
                label.length <= MaxDnsLabelLength,
                "Label '$label' exceeds max DNS label length of $MaxDnsLabelLength (was ${label.length})",
            )
        }
    }

    @Test
    fun `very long path produces valid dns labels`() {
        val path = "/very/long/path/to/some/deeply/nested/docker/socket/directory/structure/docker.sock"
        val encoded = encodeSocketPathHostname(path)
        val labels = encoded.removeSuffix(EncodedHostnameSuffix).split(".")
        for (label in labels) {
            assertTrue(
                label.length <= MaxDnsLabelLength,
                "Label '$label' exceeds max DNS label length of $MaxDnsLabelLength (was ${label.length})",
            )
        }
        assertEquals(path, decodeSocketPathHostname(encoded))
    }

    @Test
    fun `path shorter than label limit produces single label`() {
        // "/a.sock" = 7 bytes = 14 hex chars, well under 63
        val path = "/a.sock"
        val encoded = encodeSocketPathHostname(path)
        val labels = encoded.removeSuffix(EncodedHostnameSuffix).split(".")
        assertEquals(1, labels.size)
    }

    @Test
    fun `isUnixSocket returns true for unix prefix`() {
        assertTrue(isUnixSocket("unix:///var/run/docker.sock"))
    }

    @Test
    fun `isUnixSocket returns false for tcp prefix`() {
        assertTrue(!isUnixSocket("tcp://localhost:2375"))
    }
}
