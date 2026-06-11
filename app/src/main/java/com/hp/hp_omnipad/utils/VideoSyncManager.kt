package com.hp.hp_omnipad.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import com.google.gson.Gson
import com.hp.hp_omnipad.data.remote.model.VideoDto
import com.hp.hp_omnipad.ui.home.model.Hero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection

/**
 * Manages automatic downloading of all videos for offline use.
 * 
 * Storage location: Android/data/com.hp.hp_omnipad/files/Movies/OmniPad/{VideoTitle}/
 */
object VideoSyncManager {
    
    private val gson = Gson()

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    private var appContext: Context? = null
    private var currentDownloadingVideoId: String? = null
    
    // In-memory cache for folder lookups to avoid repeated filesystem scans
    private val folderCache = mutableMapOf<String, File>()

    
    /**
     * Initialize with application context to determine proper storage paths.
     */
    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }
    
    data class VideoMetadata(
        val id: String,
        val title: String,
        val description: String,
        val thumbnailUrl: String,
        val videoUrl: String,
        val duration: String,
        val categoryIds: List<String>,
        val isHero: Boolean = false,
        val downloadedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Check if a video is fully downloaded (exists and NO incomplete download lock file).
     */
    fun isVideoDownloaded(videoId: String): Boolean {
        val folder = findVideoFolder(videoId) ?: return false
        val videoFile = File(folder, "video.mp4")
        val lockFile = File(folder, "downloading.lock")
        
        // A video is complete ONLY if video.mp4 exists and NO lock file is present
        return videoFile.exists() && videoFile.length() > 1000 && !lockFile.exists()
    }

    /**
     * Get all locally downloaded video IDs.
     */
    fun getLocalVideoIds(): List<String> {
        val baseFolder = getBaseFolder()
        if (!baseFolder.exists()) return emptyList()
        
        return baseFolder.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { folder ->
                val metadataFile = File(folder, "metadata.json")
                if (metadataFile.exists()) {
                    try {
                        val metadata = gson.fromJson(metadataFile.readText(), VideoMetadata::class.java)
                        metadata.id
                    } catch (e: Exception) {
                        null
                    }
                } else null
            } ?: emptyList()
    }

    fun startSync(
        context: Context,
        videos: List<VideoDto>,
        heroes: List<Hero>
    ) {
        backgroundScope.launch {
            delay(500)
            try {
                syncAllVideos(context, videos, heroes)
            } catch (e: Exception) {
                SyncManager.updateMessage("Download paused — will retry next launch")
                delay(3000)
                SyncManager.completeSync()
            }
        }
    }
    
    /**
     * Main sync function: Processes and downloads regular videos FIRST, then non-YouTube heroes.
     * Supports resuming interrupted downloads.
     */
    suspend fun syncAllVideos(
        context: Context,
        videos: List<VideoDto>,
        heroes: List<Hero>
    ) = withContext(Dispatchers.IO) {
        
        initialize(context)
        
        val totalItems = videos.size + heroes.size
        var completedItems = 0
        var successCount = 0
        var skipCount = 0
        var failCount = 0
        
        if (totalItems == 0) return@withContext
        
        SyncManager.startSync("Downloading library for offline use...", totalItems, isDownloadingVideos = true)
        
        // Clean up folders for videos that no longer exist in Firebase
        val allFirebaseIds = videos.map { it.id } + heroes.map { it.id }
        cleanupDeletedVideos(allFirebaseIds)
        
        // 1. Regular Videos FIRST
        /* for (video in videos) {
            if (!isActive) {
                cleanupCurrentDownload()
                return@withContext
            }
            
            if (isVideoDownloaded(video.id)) {
                skipCount++
            } else {
                currentDownloadingVideoId = video.id
                
                val success = downloadVideoWithMetadata(
                    videoId = video.id,
                    videoUrl = video.videoUrl,
                    title = video.title,
                    description = video.description,
                    thumbnailUrl = video.thumbnailUrl,
                    duration = formatDuration(video.durationSec),
                    categoryIds = video.categoryIds,
                    isHero = false
                )
                
                currentDownloadingVideoId = null
                if (success) successCount++ else failCount++
            }
            completedItems++
            SyncManager.updateProgress(completedItems)
        }*/

        for (video in videos) {
            if (!isActive) { cleanupCurrentDownload(); return@withContext }

            if (isVideoDownloaded(video.id)) {
                skipCount++
                completedItems++
                SyncManager.updateProgress(completedItems)
            } else {
                // ✅ Advance counter immediately when this video starts downloading
                SyncManager.updateProgress(completedItems, message = "Downloading: ${video.title}")

                currentDownloadingVideoId = video.id
                val success = downloadVideoWithMetadata(
                    videoId = video.id,
                    videoUrl = video.videoUrl,
                    title = video.title,
                    description = video.description,
                    thumbnailUrl = video.thumbnailUrl,
                    duration = formatDuration(video.durationSec),
                    categoryIds = video.categoryIds,
                    isHero = false
                )
                currentDownloadingVideoId = null
                if (success) successCount++ else failCount++

                completedItems++
                SyncManager.updateProgress(completedItems)  // mark as fully complete
            }
        }
        
        // 2. Hero Videos SECOND (Skip YouTube heroes)
        for (hero in heroes) {
            if (!isActive) {
                cleanupCurrentDownload()
                return@withContext
            }
            
            // Skip YouTube heroes
            if (hero.isYoutubeLink) {
                skipCount++
                completedItems++
                SyncManager.updateProgress(completedItems)
                continue
            }

            if (isVideoDownloaded(hero.id)) {
                skipCount++
            } else {
                currentDownloadingVideoId = hero.id
                
                val success = downloadVideoWithMetadata(
                    videoId = hero.id,
                    videoUrl = hero.videoUrl,
                    title = hero.title,
                    description = "",
                    thumbnailUrl = hero.thumbnailUrl,
                    duration = "",
                    categoryIds = emptyList(),
                    isHero = true
                )
                
                currentDownloadingVideoId = null
                if (success) successCount++ else failCount++
            }
            completedItems++
            SyncManager.updateProgress(completedItems)
        }
        
        SyncManager.completeSync(successCount, skipCount, failCount)
    }

    /**
     * Retries missing thumbnails for existing local videos.
     */
    suspend fun retryMissingThumbnails(): Int = withContext(Dispatchers.IO) {
        val baseFolder = getBaseFolder()
        if (!baseFolder.exists()) return@withContext 0
        
        var successCount = 0
        baseFolder.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
            val thumbnailFile = File(folder, "thumbnail.jpg")
            val metadataFile = File(folder, "metadata.json")
            
            if ((!thumbnailFile.exists() || thumbnailFile.length() == 0L) && metadataFile.exists()) {
                try {
                    val metadata = gson.fromJson(metadataFile.readText(), VideoMetadata::class.java)
                    if (metadata.thumbnailUrl.isNotEmpty()) {
                        if (downloadFile(metadata.thumbnailUrl, thumbnailFile)) {
                            successCount++
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
        successCount
    }

    /**
     * Public function to check and remove videos deleted from Firebase.
     */
    suspend fun checkAndRemoveDeletedVideos(
        context: Context,
        videos: List<VideoDto>,
        heroes: List<Hero>
    ) = withContext(Dispatchers.IO) {
        initialize(context)
        val allFirebaseIds = videos.map { it.id } + heroes.map { it.id }
        cleanupDeletedVideos(allFirebaseIds)
    }

    private suspend fun downloadVideoWithMetadata(
        videoId: String,
        videoUrl: String,
        title: String,
        description: String,
        thumbnailUrl: String,
        duration: String,
        categoryIds: List<String>,
        isHero: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (videoUrl.isEmpty()) return@withContext false
        
        try {
            val folder = getOrCreateVideoFolder(videoId, title)
            val videoFile = File(folder, "video.mp4")
            val metadataFile = File(folder, "metadata.json")
            val lockFile = File(folder, "downloading.lock")
            
            // Mark as active download (lock file)
            if (!lockFile.exists()) lockFile.writeText(videoId)
            
            val metadata = VideoMetadata(videoId, title, description, thumbnailUrl, videoUrl, duration, categoryIds, isHero)
            metadataFile.writeText(gson.toJson(metadata))
            
            if (thumbnailUrl.isNotEmpty()) {
                downloadFile(thumbnailUrl, File(folder, "thumbnail.jpg"))
            }
            
            // Download Video with Resume support
            val videoSuccess = downloadFile(videoUrl, videoFile)
            if (videoSuccess && videoFile.exists() && videoFile.length() > 1000) {
                lockFile.delete() // Download COMPLETE - remove lock
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Download a file from URL with Browser User-Agent, Redirect support and Progress.
     */
    private fun downloadFile(originalUrl: String, destination: File): Boolean {
        if (originalUrl.isEmpty()) return false
        
        val urlString = convertToDirectUrl(originalUrl)
        val existingSize = if (destination.exists()) destination.length() else 0L
        val isVideo = destination.name.endsWith(".mp4")
        
        var connection: HttpURLConnection? = null
        try {
            val url = SafeUrls.toValidatedDownloadUrl(urlString)
            if (url == null) {
                return false
            }
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 120000 
            connection.readTimeout = 120000
            
            // CRITICAL: Set Browser User-Agent to avoid Google Drive consent pages/403s
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.instanceFollowRedirects = true
            
            // HTTP Range header for resuming partial downloads
            if (isVideo && existingSize > 0) {
                connection.setRequestProperty("Range", "bytes=$existingSize-")
            }
            
            connection.connect()
            
            val responseCode = connection.responseCode
            
            // Handle redirects (HttpURLConnection won't follow HTTP -> HTTPS automatically)
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == 303) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()
                return if (newUrl != null && SafeUrls.isPermittedDownloadUrl(newUrl)) {
                    downloadFile(newUrl, destination)
                } else {
                    false
                }
            }

            // 200 = Full file, 206 = Partial content (Successful resume)
            val isResuming = (responseCode == HttpURLConnection.HTTP_PARTIAL)
            if (responseCode != HttpURLConnection.HTTP_OK && !isResuming) {
                return false
            }

            //val totalContentLength = connection.contentLength.toLong() + if (isResuming) existingSize else 0L
            val rawLength: Long = connection.getHeaderField("Content-Length")?.toLongOrNull()
                ?: connection.getHeaderField("x-goog-stored-content-length")?.toLongOrNull()
                ?: connection.getHeaderField("content-range")
                    ?.substringAfterLast("/")?.toLongOrNull()
                ?: -1L
            val totalContentLength = if (rawLength > 0) {
                rawLength + if (isResuming) existingSize else 0L
            } else -1L

            connection.inputStream.use { input ->
                // Use 'append = true' if we are resuming (206)
                FileOutputStream(destination, isResuming).use { output ->
                    /* val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = if (isResuming) existingSize else 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // Update current file percentage
                        if (totalContentLength > 0 && isVideo) {
                            val progress = totalBytesRead.toFloat() / totalContentLength.toFloat()
                            SyncManager.updateFileProgress(progress)
                        }
                    }*/

                    val buffer = ByteArray(256 * 1024)  // 256KB — reduces syscalls by 32x
                    var bytesRead: Int
                    var totalBytesRead = if (isResuming) existingSize else 0L
                    var lastReportedProgress = -1f       // track last reported value

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (totalContentLength > 0 && isVideo) {
                            val progress = (totalBytesRead.toFloat() / totalContentLength.toFloat()).coerceIn(0f, 1f)
                            // Only emit when progress moves at least 1% — caps at ~100 emissions per file
                            if (progress - lastReportedProgress >= 0.01f) {
                                SyncManager.updateFileProgress(progress)
                                lastReportedProgress = progress
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            return false
        } finally {
            connection?.disconnect()
        }
    }

    private fun cleanupDeletedVideos(firebaseVideoIds: List<String>) {
        val baseFolder = getBaseFolder()
        if (!baseFolder.exists()) return
        baseFolder.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
            val metadataFile = File(folder, "metadata.json")
            if (metadataFile.exists()) {
                try {
                    val metadata = gson.fromJson(metadataFile.readText(), VideoMetadata::class.java)
                    if (!firebaseVideoIds.contains(metadata.id)) {
                        folder.deleteRecursively()
                        folderCache.remove(metadata.id)
                    }
                } catch (e: Exception) {
                    folder.deleteRecursively()
                }
            }
        }
    }

    private fun convertToDirectUrl(url: String): String {
        // Support for common Google Drive formats
        val driveFileRegex = "drive\\.google\\.com/file/d/([^/?#]+)".toRegex()
        val ucIdRegex = "id=([^&]+)".toRegex()
        
        val id = driveFileRegex.find(url)?.groupValues?.get(1) 
                 ?: ucIdRegex.find(url)?.groupValues?.get(1)
        
        return if (id != null) {
            "https://drive.google.com/uc?export=download&id=$id"
        } else {
            url
        }
    }

    private fun getBaseFolder(): File {
        val context = appContext
        val baseFolder = if (context != null) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OmniPad")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "OmniPad")
        }
        if (!baseFolder.exists()) baseFolder.mkdirs()
        return baseFolder
    }

    fun findVideoFolder(videoId: String): File? {
        val safeId = SafeFilePaths.sanitizeVideoId(videoId) ?: return null
        val baseFolder = getBaseFolder()

        folderCache[safeId]?.let { cached ->
            val folder = SafeFilePaths.validateCachedFolder(cached, baseFolder)
            if (folder != null) return folder
            folderCache.remove(safeId)
        }

        if (!baseFolder.exists()) return null
        val found = baseFolder.listFiles()?.find { folder ->
            if (!folder.isDirectory) return@find false
            if (!SafeFilePaths.isContainedIn(folder, baseFolder)) return@find false
            val metadataFile = File(folder, "metadata.json")
            if (metadataFile.exists()) {
                try {
                    val metadata = gson.fromJson(metadataFile.readText(), VideoMetadata::class.java)
                    metadata.id == safeId
                } catch (e: Exception) { false }
            } else false
        }

        if (found != null) folderCache[safeId] = found
        return found
    }

    private fun getOrCreateVideoFolder(videoId: String, title: String): File {
        val safeId = SafeFilePaths.sanitizeVideoId(videoId)
            ?: throw IllegalArgumentException("Invalid video ID")
        val existing = findVideoFolder(safeId)
        if (existing != null) return existing
        val folderName = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").replace(Regex("\\s+"), "_").trim().take(50).ifEmpty { safeId }
        val folder = File(getBaseFolder(), folderName)
        if (!folder.exists()) folder.mkdirs()
        folderCache[safeId] = folder
        return folder
    }

    private fun formatDuration(seconds: Long): String {
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    fun getLocalVideoFile(videoId: String): File? {
        val safeId = SafeFilePaths.sanitizeVideoId(videoId) ?: return null
        val folder = findVideoFolder(safeId) ?: return null
        if (!SafeFilePaths.isContainedIn(folder, getBaseFolder())) return null
        val file = SafeFilePaths.resolveChildFile(folder, "video.mp4") ?: return null
        return if (file.exists() && file.length() > 1000) file else null
    }

    fun getLocalVideoPath(videoId: String): String? {
        return getLocalVideoFile(videoId)?.absolutePath
    }

    /**
     * Returns a validated thumbnail file under the app download directory, or null.
     */
    fun getLocalThumbnailFile(videoId: String): File? {
        val safeId = SafeFilePaths.sanitizeVideoId(videoId) ?: return null
        val folder = findVideoFolder(safeId) ?: return null
        if (!SafeFilePaths.isContainedIn(folder, getBaseFolder())) return null
        val file = SafeFilePaths.resolveChildFile(folder, "thumbnail.jpg") ?: return null
        return if (file.exists() && file.length() > 0) file else null
    }

    fun getLocalThumbnailPath(videoId: String): String? {
        return getLocalThumbnailFile(videoId)?.absolutePath
    }

    fun getLocalThumbnailUri(videoId: String): Uri? {
        return getLocalThumbnailFile(videoId)?.toUri()
    }

    fun getAllDownloadedVideos(): List<VideoMetadata> {
        val baseFolder = getBaseFolder()
        if (!baseFolder.exists()) return emptyList()
        return baseFolder.listFiles()?.filter { it.isDirectory }?.mapNotNull { folder ->
            if (File(folder, "downloading.lock").exists()) return@mapNotNull null
            try {
                gson.fromJson(File(folder, "metadata.json").readText(), VideoMetadata::class.java)
            } catch (e: Exception) { null }
        } ?: emptyList()
    }

    fun cleanupCurrentDownload() {
        currentDownloadingVideoId = null
    }

    suspend fun downloadSingleVideo(
        videoId: String, videoUrl: String, title: String, description: String,
        thumbnailUrl: String, duration: String, categoryIds: List<String>, isHero: Boolean
    ): Boolean {
        return downloadVideoWithMetadata(videoId, videoUrl, title, description, thumbnailUrl, duration, categoryIds, isHero)
    }
}
