package com.hp.hp_omnipad.utils

import java.net.URL

/**
 * Centralized URL validation for outbound downloads (CWE-918 / SSRF).
 */
internal object SafeUrls {

    private val ALLOWED_HOST_SUFFIXES = listOf(
        "drive.google.com",
        "docs.google.com",
        "googleusercontent.com",
        "drive.usercontent.google.com",
        "firebasestorage.googleapis.com",
        "firebasestorage.app"
    )

    /**
     * Returns a validated [URL] for download, or null if the URL is not permitted.
     */
    fun toValidatedDownloadUrl(urlString: String): URL? {
        if (urlString.isBlank()) return null
        return try {
            val url = URL(urlString.trim())
            if (!isPermittedDownloadUrl(url)) null else url
        } catch (e: Exception) {
            null
        }
    }

    fun isPermittedDownloadUrl(urlString: String): Boolean =
        toValidatedDownloadUrl(urlString) != null

    private fun isPermittedDownloadUrl(url: URL): Boolean {
        val protocol = url.protocol.lowercase()
        if (protocol != "http" && protocol != "https") return false

        val host = url.host?.lowercase()?.trim('.') ?: return false
        if (host.isEmpty() || host == "localhost" || host.endsWith(".local")) return false
        if (isPrivateOrLoopbackHost(host)) return false
        return isAllowedHost(host)
    }

    private fun isAllowedHost(host: String): Boolean =
        ALLOWED_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }

    private fun isPrivateOrLoopbackHost(host: String): Boolean {
        if (host.startsWith('[') && host.endsWith(']')) {
            val ipv6 = host.substring(1, host.length - 1).lowercase()
            return ipv6 == "::1" ||
                ipv6.startsWith("fe80:") ||
                ipv6.startsWith("fc") ||
                ipv6.startsWith("fd")
        }
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
            val a = parts[0].toInt()
            val b = parts[1].toInt()
            return when {
                a == 127 -> true
                a == 10 -> true
                a == 172 && b in 16..31 -> true
                a == 192 && b == 168 -> true
                a == 169 && b == 254 -> true
                a == 0 -> true
                else -> false
            }
        }
        return false
    }
}
