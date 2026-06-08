package com.hp.hp_omnipad.utils

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebViewClient that merges recommended HTTP security response headers (CWE-693).
 */
internal class SecureWebViewClient : WebViewClient() {

    private val trustedThirdPartyHosts = listOf(
        "youtube.com",
        "youtube-nocookie.com",
        "ytimg.com",
        "gstatic.com",
        "google.com",
        "googleapis.com",
        "googleusercontent.com"
    )

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (isTrustedThirdParty(request.url.host)) {
            return super.shouldInterceptRequest(view, request)
        }
        val original = super.shouldInterceptRequest(view, request) ?: return null
        return withSecurityHeaders(original)
    }

    private fun isTrustedThirdParty(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val normalized = host.lowercase().trim('.')
        return trustedThirdPartyHosts.any { suffix ->
            normalized == suffix || normalized.endsWith(".$suffix")
        }
    }

    private fun withSecurityHeaders(original: WebResourceResponse): WebResourceResponse {
        val headers = SecurityResponseHeaders.mergeInto(original.responseHeaders)
        return WebResourceResponse(
            original.mimeType,
            original.encoding,
            original.statusCode,
            original.reasonPhrase ?: "OK",
            headers,
            original.data
        )
    }
}
