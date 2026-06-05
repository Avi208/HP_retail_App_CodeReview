package com.hp.hp_omnipad.utils

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileDownloader {

    private const val APP_FOLDER = "OmniPad"
    private const val METADATA_FILE = "metadata.txt"
    private const val VIDEO_ID_FILE = "video_id.txt"
    
    // App context for accessing external files directory
    private var appContext: Context? = null
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Sanitize filename by removing invalid characters
     */
    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100) // Limit length
    }

    /**
     * Returns base folder for all downloads (public access for cleanup)
     * Uses app's private external storage which doesn't require permissions on Android 10+
     */
    fun getDownloadFolder(): File = getBaseFolder()
    
    private fun getBaseFolder(): File {
        val context = appContext
        
        val baseFolder = if (context != null) {
            val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            File(moviesDir, APP_FOLDER)
        } else {
            // Fallback (shouldn't happen)
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                APP_FOLDER
            )
        }
        
        if (!baseFolder.exists()) {
            baseFolder.mkdirs()
        }
        return baseFolder
    }

    /**
     * Returns folder for a specific video using title
     */
    private fun getVideoFolder(videoId: String, title: String? = null): File {
        val baseFolder = getBaseFolder()
        
        // Try to find existing folder by video ID
        val existingFolder = findFolderByVideoId(videoId)
        if (existingFolder != null && existingFolder.exists()) {
            return existingFolder
        }

        // Create new folder using sanitized title or fall back to ID
        val folderName = if (title != null && title.isNotBlank()) {
            sanitizeFilename(title)
        } else {
            videoId
        }

        val videoFolder = File(baseFolder, folderName)
        if (!videoFolder.exists()) {
            videoFolder.mkdirs()
        }

        return videoFolder
    }

    /**
     * Find existing folder by video ID (searches metadata)
     */
    private fun findFolderByVideoId(videoId: String): File? {
        val baseFolder = getBaseFolder()
        if (!baseFolder.exists()) return null
        
        return baseFolder.listFiles()?.find { folder ->
            if (!folder.isDirectory) return@find false
            
            // Check metadata.json first (new format from VideoSyncManager)
            val metadataFile = File(folder, "metadata.json")
            if (metadataFile.exists()) {
                try {
                    val json = metadataFile.readText()
                    val idMatch = "\"id\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(json)
                    if (idMatch != null && idMatch.groupValues[1] == videoId) {
                        return@find true
                    }
                } catch (e: Exception) {
                    // Continue to other checks
                }
            }
            
            // Check video_id.txt (legacy format)
            val idFile = File(folder, VIDEO_ID_FILE)
            if (idFile.exists()) {
                return@find idFile.readText().trim() == videoId
            }
            
            // Check folder name as last resort
            folder.name == videoId
        }
    }

    /**
     * Save video ID to folder for lookup
     */
    private fun saveVideoId(folder: File, videoId: String) {
        try {
            val idFile = File(folder, VIDEO_ID_FILE)
            idFile.writeText(videoId)
        } catch (e: Exception) {
            Log.e("FileDownloader", "Failed to save video ID: ${e.message}")
        }
    }

    /**
     * Check if video already downloaded
     */
    fun isVideoDownloaded(context: Context, videoId: String): Boolean {
        val folder = findFolderByVideoId(videoId)
        if (folder != null) {
            val videoFile = File(folder, "video.mp4")
            return videoFile.exists()
        }
        return false
    }

    /**
     * Download video file with title metadata
     */
    fun downloadVideo(
        context: Context,
        url: String,
        videoId: String,
        title: String = videoId
    ): Long {
        
        val folder = getVideoFolder(videoId, title)
        val videoFile = File(folder, "video.mp4")

        if (videoFile.exists()) {
            Log.d("FileDownloader", "Video already downloaded")
            return -1
        }

        // Save video ID for lookup and metadata for display
        saveVideoId(folder, videoId)
        saveMetadata(folder, title)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading: $title")
            .setDescription("Omni Pad Video")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationUri(Uri.fromFile(videoFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val manager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadId = manager.enqueue(request)

        Log.d("FileDownloader", "Download started with id: $downloadId for video: $title to folder: ${folder.name}")

        return downloadId
    }

    /**
     * Download thumbnail directly (without DownloadManager to avoid notification)
     */
    suspend fun downloadThumbnail(
        thumbnailUrl: String,
        videoId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isEmpty()) {
            Log.w("FileDownloader", "Thumbnail URL is empty for video: $videoId")
            return@withContext false
        }

        try {
            val folder = findFolderByVideoId(videoId)
            if (folder == null) {
                Log.e("FileDownloader", "Video folder not found for ID: $videoId")
                return@withContext false
            }
            
            val thumbnailFile = File(folder, "thumbnail.jpg")

            if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                Log.d("FileDownloader", "Thumbnail already exists: ${thumbnailFile.absolutePath}")
                return@withContext true
            }

            // Save thumbnail URL for retry attempts
            saveThumbnailUrl(videoId, thumbnailUrl)

            Log.d("FileDownloader", "Starting thumbnail download from: $thumbnailUrl to ${thumbnailFile.absolutePath}")

            // Download directly using URL connection with timeout
            val url = SafeUrls.toValidatedDownloadUrl(thumbnailUrl)
            if (url == null) {
                Log.w("FileDownloader", "Rejected thumbnail URL for video: $videoId")
                return@withContext false
            }
            val connection = url.openConnection().apply {
                connectTimeout = 30000
                readTimeout = 30000
            }
            connection.connect()

            val inputStream = connection.getInputStream()
            val outputStream = thumbnailFile.outputStream()

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("FileDownloader", "Thumbnail downloaded successfully to: ${thumbnailFile.absolutePath}, size: ${thumbnailFile.length()}")
            true
        } catch (e: Exception) {
            Log.e("FileDownloader", "Failed to download thumbnail: ${e.message}", e)
            false
        }
    }

    /**
     * Save thumbnail URL for retry downloads
     */
    private fun saveThumbnailUrl(videoId: String, url: String) {
        try {
            val folder = findFolderByVideoId(videoId) ?: return
            val urlFile = File(folder, "thumbnail_url.txt")
            urlFile.writeText(url)
        } catch (e: Exception) {
            Log.e("FileDownloader", "Failed to save thumbnail URL: ${e.message}")
        }
    }

    /**
     * Get saved thumbnail URL
     */
    fun getThumbnailUrl(videoId: String): String? {
        return try {
            val folder = findFolderByVideoId(videoId) ?: return null
            val urlFile = File(folder, "thumbnail_url.txt")
            if (urlFile.exists()) urlFile.readText().trim() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Try to download missing thumbnail
     */
    suspend fun retryThumbnailDownload(videoId: String): Boolean {
        val thumbnailFile = getThumbnailFile(videoId)
        if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
            return true
        }
        
        val url = getThumbnailUrl(videoId) ?: return false
        return downloadThumbnail(url, videoId)
    }
    
    /**
     * Get download progress for a download ID
     * Returns progress percentage (0-100) or -1 if download failed/not found
     */
    fun getDownloadProgress(context: Context, downloadId: Long): DownloadProgress {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        
        var cursor: Cursor? = null
        try {
            cursor = manager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                
                val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                val bytesTotal = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else 0L
                
                val progress = if (bytesTotal > 0) {
                    ((bytesDownloaded * 100) / bytesTotal).toInt()
                } else {
                    0
                }
                
                return when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadProgress(100, DownloadStatus.COMPLETED)
                    DownloadManager.STATUS_FAILED -> DownloadProgress(0, DownloadStatus.FAILED)
                    DownloadManager.STATUS_RUNNING -> DownloadProgress(progress, DownloadStatus.DOWNLOADING)
                    DownloadManager.STATUS_PENDING -> DownloadProgress(0, DownloadStatus.PENDING)
                    DownloadManager.STATUS_PAUSED -> DownloadProgress(progress, DownloadStatus.PAUSED)
                    else -> DownloadProgress(0, DownloadStatus.UNKNOWN)
                }
            }
        } finally {
            cursor?.close()
        }
        
        return DownloadProgress(0, DownloadStatus.UNKNOWN)
    }

    /**
     * Save video metadata (title) for offline display
     */
    private fun saveMetadata(folder: File, title: String) {
        try {
            val metadataFile = File(folder, METADATA_FILE)
            metadataFile.writeText(title)
        } catch (e: Exception) {
            Log.e("FileDownloader", "Failed to save metadata: ${e.message}")
        }
    }

    /**
     * Get video title from folder metadata
     */
    private fun getVideoTitleFromFolder(folder: File): String {
        return try {
            // Try new metadata.json format first
            val jsonFile = File(folder, "metadata.json")
            if (jsonFile.exists()) {
                val json = jsonFile.readText()
                val titleMatch = "\"title\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(json)
                if (titleMatch != null) {
                    return titleMatch.groupValues[1]
                }
            }
            
            // Fall back to old metadata.txt format
            val metadataFile = File(folder, METADATA_FILE)
            if (metadataFile.exists()) {
                metadataFile.readText().trim()
            } else {
                folder.name
            }
        } catch (e: Exception) {
            folder.name
        }
    }

    /**
     * Get video title by video ID
     */
    fun getVideoTitle(videoId: String): String {
        val folder = findFolderByVideoId(videoId) ?: return videoId
        return getVideoTitleFromFolder(folder)
    }

    /**
     * Get thumbnail file for offline UI
     */
    fun getThumbnailFile(videoId: String): File {
        val folder = findFolderByVideoId(videoId) ?: File(getBaseFolder(), videoId)
        return File(folder, "thumbnail.jpg")
    }

    /**
     * Get video file
     */
    fun getVideoFile(videoId: String): File {
        val folder = findFolderByVideoId(videoId) ?: File(getBaseFolder(), videoId)
        return File(folder, "video.mp4")
    }

    /**
     * Delete entire video folder (including video and thumbnail)
     */
    fun deleteVideo(videoId: String) {
        Log.d("FileDownloader", "Attempting to delete video: $videoId")
        Log.d("FileDownloader", "Base folder: ${getBaseFolder().absolutePath}")
        
        val folder = findFolderByVideoId(videoId)
        if (folder != null && folder.exists()) {
            Log.d("FileDownloader", "Found folder to delete: ${folder.absolutePath}")
            val deleted = folder.deleteRecursively()
            Log.d("FileDownloader", "Deleted folder ${folder.name}: $deleted")
        } else {
            Log.w("FileDownloader", "Folder not found for video ID: $videoId")
        }
    }

    /**
     * Get video ID from folder (checks both old and new formats)
     */
    private fun getVideoIdFromFolder(folder: File): String {
        // Try new metadata.json format first
        val metadataFile = File(folder, "metadata.json")
        if (metadataFile.exists()) {
            try {
                val json = metadataFile.readText()
                val idMatch = "\"id\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(json)
                if (idMatch != null) {
                    return idMatch.groupValues[1]
                }
            } catch (e: Exception) {
                Log.e("FileDownloader", "Failed to parse metadata.json: ${e.message}")
            }
        }
        
        // Fall back to old video_id.txt format
        val idFile = File(folder, VIDEO_ID_FILE)
        return if (idFile.exists()) {
            idFile.readText().trim()
        } else {
            folder.name
        }
    }

    /**
     * Get all downloaded video folders
     * Excludes incomplete downloads (those with lock file)
     */
    fun getAllDownloadedVideos(): List<DownloadedVideo> {
        val baseFolder = getBaseFolder()

        if (!baseFolder.exists()) return emptyList()

        return baseFolder.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { folder ->
                val videoFile = File(folder, "video.mp4")
                val thumbnailFile = File(folder, "thumbnail.jpg")
                val lockFile = File(folder, "downloading.lock")
                val videoId = getVideoIdFromFolder(folder)
                
                // Skip incomplete downloads
                if (lockFile.exists()) {
                    Log.d("FileDownloader", "Skipping incomplete download: ${folder.name}")
                    return@mapNotNull null
                }

                if (videoFile.exists() && videoFile.length() > 1000) {
                    DownloadedVideo(
                        id = videoId,
                        title = getVideoTitleFromFolder(folder),
                        videoPath = videoFile.absolutePath,
                        thumbnailPath = if (thumbnailFile.exists() && thumbnailFile.length() > 0) 
                            thumbnailFile.absolutePath else null,
                        fileSize = videoFile.length()
                    )
                } else null
            }
            ?: emptyList()
    }

    /**
     * Download PDF / resource files
     */
    fun downloadResourceFile(
        context: Context,
        url: String,
        fileName: String
    ): Long {

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "$APP_FOLDER/$fileName"
            )

        val manager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        return manager.enqueue(request)
    }
}

data class DownloadedVideo(
    val id: String,
    val title: String,
    val videoPath: String,
    val thumbnailPath: String?,
    val fileSize: Long
)

data class DownloadProgress(
    val percentage: Int,
    val status: DownloadStatus
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    UNKNOWN
}