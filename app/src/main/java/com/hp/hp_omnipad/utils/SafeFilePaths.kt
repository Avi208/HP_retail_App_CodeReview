package com.hp.hp_omnipad.utils

import java.io.File
import java.nio.file.Path

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
        return rebuildAllowlistedFileName(fileName)
    }

    /**
     * Rebuilds a filename from allowlisted characters only (breaks external taint for static analysis).
     */
    private fun rebuildAllowlistedFileName(fileName: String): String? {
        if (fileName.isBlank() || fileName.length > 255) return null
        val rebuilt = buildString(fileName.length) {
            for (ch in fileName) {
                when (ch) {
                    in 'a'..'z', in 'A'..'Z', in '0'..'9' -> append(ch)
                    '.', '_', '-' -> append(ch)
                    else -> return null
                }
            }
        }
        if (rebuilt == "." || rebuilt == ".." || rebuilt.contains("..")) return null
        if (!rebuilt.matches(SAFE_FILENAME)) return null
        return rebuilt
    }

    fun resolveChildFile(baseDir: File, childName: String): File? {
        val safeName = rebuildAllowlistedFileName(childName) ?: return null
        return resolvePathUnderBase(baseDir.canonicalFile, safeName)
    }

    /**
     * Validates a cached [folder] reference (not a path string) is under [allowedBase].
     */
    fun validateCachedFolder(folder: File, allowedBase: File): File? {
        return try {
            if (!folder.exists() || !folder.isDirectory) return null
            val canonical = folder.canonicalFile
            val baseCanonical = allowedBase.canonicalFile
            if (isContainedIn(canonical, baseCanonical)) canonical else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves a single allowlisted filename under a canonical base using NIO Path (no File(parent, name)).
     */
    private fun resolvePathUnderBase(baseCanonical: File, allowlistedName: String): File? {
        return try {
            val basePath: Path = baseCanonical.toPath().toAbsolutePath().normalize()
            val resolved = basePath.resolve(allowlistedName).normalize()
            if (!resolved.startsWith(basePath)) return null
            val canonical = resolved.toFile().canonicalFile
            if (isContainedIn(canonical, baseCanonical)) canonical else null
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
