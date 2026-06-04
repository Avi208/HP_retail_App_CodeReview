package com.hp.hp_omnipad.utils

import java.io.File

/**
 * Centralized path validation for file operations using external IDs (CWE-73).
 */
internal object SafeFilePaths {

    fun sanitizeVideoId(videoId: String): String? {
        if (videoId.isBlank() || videoId.length > 128) return null
        if (!videoId.matches(Regex("^[a-zA-Z0-9_-]+$"))) return null
        return videoId
    }

    fun resolveChildFile(baseDir: File, childName: String): File? {
        if (childName.isBlank() || childName.contains("..")) return null
        if (childName.contains('/') || childName.contains('\\')) return null
        return try {
            val dirCanonical = baseDir.canonicalFile
            val file = File(dirCanonical, childName).canonicalFile
            val basePath = dirCanonical.absolutePath
            val filePath = file.absolutePath
            if (filePath == basePath || !filePath.startsWith("$basePath${File.separator}")) {
                null
            } else {
                file
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isContainedIn(file: File, baseDir: File): Boolean {
        return try {
            val basePath = baseDir.canonicalFile.absolutePath
            val filePath = file.canonicalFile.absolutePath
            filePath == basePath || filePath.startsWith("$basePath${File.separator}")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resolves an absolute local path only if the file exists and lies under one of [allowedBases].
     */
    fun resolveLocalPlaybackFile(path: String, allowedBases: List<File>): File? {
        if (path.isBlank() || !path.startsWith("/")) return null
        return try {
            val file = File(path).canonicalFile
            if (!file.exists() || !file.isFile) return null
            if (allowedBases.any { isContainedIn(file, it) }) file else null
        } catch (e: Exception) {
            null
        }
    }
}
