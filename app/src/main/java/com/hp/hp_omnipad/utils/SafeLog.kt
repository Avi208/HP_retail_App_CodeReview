package com.hp.hp_omnipad.utils

import android.util.Log

/**
 * Sanitizes untrusted values before writing to logs (CWE-117 / CRLF injection).
 * All dynamic log output should go through [d], [w], or [e] so sanitization is centralized.
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

    fun d(tag: String, block: SafeMessageBuilder.() -> Unit) {
        Log.d(tag, SafeMessageBuilder().apply(block).toString())
    }

    fun w(tag: String, block: SafeMessageBuilder.() -> Unit) {
        Log.w(tag, SafeMessageBuilder().apply(block).toString())
    }

    fun e(tag: String, block: SafeMessageBuilder.() -> Unit) {
        Log.e(tag, SafeMessageBuilder().apply(block).toString())
    }

    fun e(tag: String, throwable: Throwable?, block: SafeMessageBuilder.() -> Unit) {
        Log.e(tag, SafeMessageBuilder().apply(block).toString(), throwable)
    }

    class SafeMessageBuilder {
        private val sb = StringBuilder()

        fun text(literal: String) {
            sb.append(literal)
        }

        fun value(untrusted: String?) {
            sb.append(sanitize(untrusted))
        }

        fun value(untrusted: Number) {
            sb.append(untrusted)
        }

        fun value(untrusted: Boolean) {
            sb.append(untrusted)
        }

        override fun toString(): String = sb.toString()
    }
}
