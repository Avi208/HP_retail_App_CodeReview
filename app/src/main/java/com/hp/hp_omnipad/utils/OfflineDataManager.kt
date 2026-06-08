package com.hp.hp_omnipad.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hp.hp_omnipad.data.remote.model.CategoryDto
import com.hp.hp_omnipad.data.remote.model.VideoDto
import com.hp.hp_omnipad.ui.home.model.Hero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object OfflineDataManager {
    
    private const val TAG = "OfflineDataManager"
    private const val VIDEOS_CACHE_FILE = "videos_cache.json"
    private const val CATEGORIES_CACHE_FILE = "categories_cache.json"
    private const val HEROES_CACHE_FILE = "heroes_cache.json"
    private const val THUMBNAILS_FOLDER = "thumbnails"
    
    private val gson = Gson()
    
    // ============ VIDEO CACHE ============
    
    /**
     * Save videos to local cache
     */
    suspend fun saveVideosToCache(context: Context, videos: List<VideoDto>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, VIDEOS_CACHE_FILE)
            val json = gson.toJson(videos)
            file.writeText(json)
            SafeLog.d(TAG) { text("Saved "); value(videos.size); text(" videos to cache") }
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to save videos to cache: "); value(e.message) }
        }
    }
    
    /**
     * Load videos from local cache
     */
    suspend fun loadVideosFromCache(context: Context): List<VideoDto>? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, VIDEOS_CACHE_FILE)
            if (!file.exists()) return@withContext null
            
            val json = file.readText()
            val type = object : TypeToken<List<VideoDto>>() {}.type
            val videos: List<VideoDto> = gson.fromJson(json, type)
            SafeLog.d(TAG) { text("Loaded "); value(videos.size); text(" videos from cache") }
            videos
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to load videos from cache: "); value(e.message) }
            null
        }
    }
    
    // ============ CATEGORY CACHE ============
    
    /**
     * Save categories to local cache
     */
    suspend fun saveCategoriesToCache(context: Context, categories: List<CategoryDto>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, CATEGORIES_CACHE_FILE)
            val json = gson.toJson(categories)
            file.writeText(json)
            SafeLog.d(TAG) { text("Saved "); value(categories.size); text(" categories to cache") }
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to save categories to cache: "); value(e.message) }
        }
    }
    
    /**
     * Load categories from local cache
     */
    suspend fun loadCategoriesFromCache(context: Context): List<CategoryDto>? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, CATEGORIES_CACHE_FILE)
            if (!file.exists()) return@withContext null
            
            val json = file.readText()
            val type = object : TypeToken<List<CategoryDto>>() {}.type
            val categories: List<CategoryDto> = gson.fromJson(json, type)
            SafeLog.d(TAG) { text("Loaded "); value(categories.size); text(" categories from cache") }
            categories
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to load categories from cache: "); value(e.message) }
            null
        }
    }
    
    // ============ HERO CACHE ============
    
    /**
     * Save heroes to local cache
     */
    suspend fun saveHeroesToCache(context: Context, heroes: List<Hero>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, HEROES_CACHE_FILE)
            val json = gson.toJson(heroes)
            file.writeText(json)
            SafeLog.d(TAG) { text("Saved "); value(heroes.size); text(" heroes to cache") }
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to save heroes to cache: "); value(e.message) }
        }
    }
    
    /**
     * Load heroes from local cache
     */
    suspend fun loadHeroesFromCache(context: Context): List<Hero>? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, HEROES_CACHE_FILE)
            if (!file.exists()) return@withContext null
            
            val json = file.readText()
            val type = object : TypeToken<List<Hero>>() {}.type
            val heroes: List<Hero> = gson.fromJson(json, type)
            SafeLog.d(TAG) { text("Loaded "); value(heroes.size); text(" heroes from cache") }
            heroes
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to load heroes from cache: "); value(e.message) }
            null
        }
    }
    
    // ============ THUMBNAIL CACHE ============
    
    /**
     * Get thumbnail folder
     */
    private fun getThumbnailFolder(context: Context): File {
        val folder = File(context.filesDir, THUMBNAILS_FOLDER)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }
    
    /**
     * Resolves a thumbnail file only under [getThumbnailFolder]; returns null if invalid.
     */
    private fun resolveThumbnailFile(context: Context, videoId: String): File? {
        val safeId = SafeFilePaths.sanitizeVideoId(videoId) ?: return null
        val file = SafeFilePaths.resolveChildFile(getThumbnailFolder(context), "$safeId.jpg")
        if (file == null) {
            SafeLog.w(TAG) { text("Rejected thumbnail path for video id: "); value(videoId) }
        }
        return file
    }
    
    /**
     * Get local thumbnail path for a video
     */
    fun getLocalThumbnailPath(context: Context, videoId: String): String {
        return resolveThumbnailFile(context, videoId)?.absolutePath ?: ""
    }
    
    /**
     * Check if thumbnail is cached locally
     */
    fun isThumbnailCached(context: Context, videoId: String): Boolean {
        val file = resolveThumbnailFile(context, videoId) ?: return false
        return file.exists() && file.length() > 0
    }
    
    /**
     * Download and cache a thumbnail
     */
    suspend fun downloadAndCacheThumbnail(
        context: Context,
        videoId: String,
        thumbnailUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isEmpty()) return@withContext false
        
        val thumbnailFile = resolveThumbnailFile(context, videoId) ?: return@withContext false
        
        try {
            if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                return@withContext true
            }
            
            val url = SafeUrls.toValidatedDownloadUrl(thumbnailUrl)
                ?: return@withContext false
            val connection = url.openConnection().apply {
                connectTimeout = 30000
                readTimeout = 30000
            }
            connection.connect()
            
            connection.getInputStream().use { input ->
                thumbnailFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            SafeLog.d(TAG) { text("Cached thumbnail for video: "); value(videoId) }
            true
        } catch (e: Exception) {
            SafeLog.e(TAG) {
                text("Failed to cache thumbnail for "); value(videoId)
                text(": "); value(e.message)
            }
            false
        }
    }
    
    /**
     * Get the effective thumbnail URL (local if cached, remote otherwise)
     */
    fun getEffectiveThumbnailUrl(context: Context, videoId: String, remoteUrl: String): String {
        val file = resolveThumbnailFile(context, videoId)
        return if (file != null && file.exists() && file.length() > 0) {
            "file://${file.absolutePath}"
        } else {
            remoteUrl
        }
    }
    
    // ============ SYNC UTILITIES ============
    
    /**
     * Get list of video IDs that need to be synced (not in local cache)
     */
    fun getVideosToSync(
        remoteVideos: List<VideoDto>,
        cachedVideos: List<VideoDto>?
    ): List<VideoDto> {
        if (cachedVideos == null) return remoteVideos
        
        val cachedIds = cachedVideos.map { it.id }.toSet()
        return remoteVideos.filter { it.id !in cachedIds }
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            File(context.filesDir, VIDEOS_CACHE_FILE).delete()
            File(context.filesDir, CATEGORIES_CACHE_FILE).delete()
            File(context.filesDir, HEROES_CACHE_FILE).delete()
            getThumbnailFolder(context).deleteRecursively()
            SafeLog.d(TAG) { text("Cleared all cache") }
        } catch (e: Exception) {
            SafeLog.e(TAG) { text("Failed to clear cache: "); value(e.message) }
        }
    }
    
    /**
     * Check if cache exists
     */
    fun hasCachedData(context: Context): Boolean {
        return File(context.filesDir, VIDEOS_CACHE_FILE).exists()
    }
}
