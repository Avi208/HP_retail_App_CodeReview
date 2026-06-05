package com.hp.hp_omnipad.utils

/**
 * Sanitizes untrusted values before writing to logs (CWE-117 / CRLF injection).
 */
internal object SafeLog {

    private const val MAX_LENGTH = 512

    fun sanitize(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val cleaned = buildString(value.length) {
            for (ch in value) {
                when {
                    ch == '\r' || ch == '\n' -> append('_')
                    ch.isISOControl() -> Unit
                    else -> append(ch)
                }
            }
        }
        return if (cleaned.length <= MAX_LENGTH) cleaned else cleaned.take(MAX_LENGTH) + "..."
    }
}
