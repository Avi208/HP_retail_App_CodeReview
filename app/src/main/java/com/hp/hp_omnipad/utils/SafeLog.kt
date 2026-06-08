package com.hp.hp_omnipad.utils

import android.util.Log

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

    fun d(tag: String, message: String) = Log.d(tag, message)

    fun d(tag: String, format: String, vararg args: Any?) =
        Log.d(tag, formatMessage(format, args))

    fun w(tag: String, message: String) = Log.w(tag, message)

    fun w(tag: String, format: String, vararg args: Any?) =
        Log.w(tag, formatMessage(format, args))

    fun e(tag: String, message: String) = Log.e(tag, message)

    fun e(tag: String, message: String, throwable: Throwable) =
        Log.e(tag, message, throwable)

    fun e(tag: String, format: String, vararg args: Any?) =
        Log.e(tag, formatMessage(format, args))

    fun e(tag: String, format: String, throwable: Throwable, vararg args: Any?) =
        Log.e(tag, formatMessage(format, args), throwable)

    private fun formatMessage(format: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return format

        val segments = format.split("%s")
        if (segments.size != args.size + 1) {
            return sanitize(format)
        }

        return buildString {
            segments.forEachIndexed { index, segment ->
                append(segment)
                if (index < args.size) {
                    append(sanitizeArg(args[index]))
                }
            }
        }
    }

    private fun sanitizeArg(value: Any?): String = when (value) {
        null -> ""
        is String -> sanitize(value)
        is Number, is Boolean -> value.toString()
        else -> sanitize(value.toString())
    }
}
