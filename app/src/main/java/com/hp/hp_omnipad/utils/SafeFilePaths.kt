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
        if (fileName.contains('/') || fileName.contains('\\')) return null
        if (fileName == "." || fileName == ".." || fileName.contains("..")) return null
        if (!fileName.matches(SAFE_FILENAME)) return null
        return fileName
    }

    private fun sanitizePathSegment(segment: String): String? {
        if (segment.isBlank() || segment.length > 255) return null
        if (segment == "." || segment == "..") return null
        if (segment.contains('/') || segment.contains('\\') || segment.contains('\u0000')) return null
        return segment
    }

    private fun hasParentReference(path: String): Boolean {
        return path.split('/', '\\').any { it == ".." }
    }

    fun resolveChildFile(baseDir: File, childName: String): File? {
        val safeName = sanitizeFileName(childName) ?: return null
        return try {
            val baseCanonical = baseDir.canonicalFile
            val childFile = File(baseCanonical, safeName)
            val canonicalChild = childFile.canonicalFile
            if (isContainedIn(canonicalChild, baseCanonical)) canonicalChild else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves a cached absolute directory path only if it exists and lies under [allowedBase].
     * Rebuilds the path from the trusted base instead of constructing File from the raw string.
     */
    fun resolveCachedDirectory(absolutePath: String, allowedBase: File): File? {
        if (absolutePath.isBlank() || hasParentReference(absolutePath)) return null
        return try {
            val baseCanonical = allowedBase.canonicalFile
            val basePath = baseCanonical.absolutePath
            if (absolutePath != basePath && !absolutePath.startsWith("$basePath${File.separator}")) {
                return null
            }
            val relativePath = if (absolutePath == basePath) {
                ""
            } else {
                absolutePath.substring(basePath.length).trimStart(File.separatorChar)
            }
            if (hasParentReference(relativePath)) return null

            val dir = resolveUnderBase(baseCanonical, relativePath) ?: return null
            val canonicalDir = dir.canonicalFile
            if (!canonicalDir.isDirectory || !canonicalDir.exists()) return null
            if (isContainedIn(canonicalDir, baseCanonical)) canonicalDir else null
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveUnderBase(baseCanonical: File, relativePath: String): File? {
        if (relativePath.isEmpty()) return baseCanonical
        var current = baseCanonical
        for (segment in relativePath.split(File.separatorChar)) {
            if (segment.isEmpty() || segment == ".") continue
            val safeSegment = sanitizePathSegment(segment) ?: return null
            current = File(current, safeSegment)
        }
        return current
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
