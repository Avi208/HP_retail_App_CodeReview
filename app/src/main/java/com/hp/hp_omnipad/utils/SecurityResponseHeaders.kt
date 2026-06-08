package com.hp.hp_omnipad.utils

/**
 * Recommended HTTP response security headers (CWE-693 / Protection Mechanism Failure).
 */
internal object SecurityResponseHeaders {

    const val STRICT_TRANSPORT_SECURITY = "max-age=31536000; includeSubDomains"
    const val X_CONTENT_TYPE_OPTIONS = "nosniff"
    const val X_FRAME_OPTIONS = "SAMEORIGIN"
    const val REFERRER_POLICY = "strict-origin-when-cross-origin"
    const val X_XSS_PROTECTION = "1; mode=block"

    const val YOUTUBE_EMBED_CSP =
        "default-src 'none'; " +
            "script-src 'unsafe-inline' https://www.youtube.com https://youtube.com " +
            "https://s.ytimg.com https://www.gstatic.com; " +
            "frame-src https://www.youtube.com https://youtube.com " +
            "https://www.youtube-nocookie.com; " +
            "style-src 'unsafe-inline'; " +
            "img-src https: data:; " +
            "connect-src https://www.youtube.com https://youtube.com " +
            "https://www.youtube-nocookie.com https://*.googleapis.com"

    fun forEmbedPage(): Map<String, String> = linkedMapOf(
        "Strict-Transport-Security" to STRICT_TRANSPORT_SECURITY,
        "X-Content-Type-Options" to X_CONTENT_TYPE_OPTIONS,
        "X-Frame-Options" to X_FRAME_OPTIONS,
        "Content-Security-Policy" to YOUTUBE_EMBED_CSP,
        "Referrer-Policy" to REFERRER_POLICY,
        "X-XSS-Protection" to X_XSS_PROTECTION
    )

    fun mergeInto(existing: Map<String, String>?): Map<String, String> {
        val merged = linkedMapOf<String, String>()
        existing?.let { merged.putAll(it) }
        forEmbedPage().forEach { (name, value) ->
            if (!merged.containsKey(name)) {
                merged[name] = value
            }
        }
        return merged
    }
}
