package com.hp.hp_omnipad.utils

import java.io.File

/**
 * Centralized path validation for file operations using external IDs (CWE-73).
 */
internal object SafeFilePaths {

    private val SAFE_FILENAME = Regex("^[a-zA-Z0-9._-]+$")

    fun sanitizeVideoId(videoId: String): String? {
        if (videoId.isBlank() || videoId.length > 128) return null
        if (!videoId.matches(Regex("^[a-zA-Z0-9_-]+$"))) return null
        return videoId
    }

    /**
     * Allowlist validation for a single path segment (no directory separators).
     */
    fun sanitizeFileName(fileName: String): String? {
        if (fileName.isBlank() || fileName.length > 255) return null
        val baseName = File(fileName).name
        if (baseName != fileName) return null
        if (!baseName.matches(SAFE_FILENAME)) return null
        return baseName
    }

    fun resolveChildFile(baseDir: File, childName: String): File? {
        val safeName = sanitizeFileName(childName) ?: return null
        return try {
            val basePath = baseDir.canonicalFile.toPath().normalize()
            val resolved = basePath.resolve(safeName).normalize()
            if (resolved == basePath || !resolved.startsWith(basePath)) {
                null
            } else {
                resolved.toFile()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves a cached absolute directory path only if it exists and lies under [allowedBase].
     */
    fun resolveCachedDirectory(absolutePath: String, allowedBase: File): File? {
        if (absolutePath.isBlank()) return null
        return try {
            val dir = File(absolutePath).canonicalFile
            if (!dir.isDirectory || !dir.exists()) return null
            if (isContainedIn(dir, allowedBase)) dir else null
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
