package com.hp.hp_omnipad.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hp.hp_omnipad.data.remote.model.CategoryDto
import com.hp.hp_omnipad.data.remote.model.VideoDto
import com.hp.hp_omnipad.ui.home.model.Hero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

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
            Log.d(TAG, "Saved ${videos.size} videos to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save videos to cache: ${e.message}")
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
            Log.d(TAG, "Loaded ${videos.size} videos from cache")
            videos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load videos from cache: ${e.message}")
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
            Log.d(TAG, "Saved ${categories.size} categories to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save categories to cache: ${e.message}")
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
            Log.d(TAG, "Loaded ${categories.size} categories from cache")
            categories
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load categories from cache: ${e.message}")
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
            Log.d(TAG, "Saved ${heroes.size} heroes to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save heroes to cache: ${e.message}")
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
            Log.d(TAG, "Loaded ${heroes.size} heroes from cache")
            heroes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load heroes from cache: ${e.message}")
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
     * Get local thumbnail path for a video
     */
    fun getLocalThumbnailPath(context: Context, videoId: String): String {
        return File(getThumbnailFolder(context), "$videoId.jpg").absolutePath
    }
    
    /**
     * Check if thumbnail is cached locally
     */
    fun isThumbnailCached(context: Context, videoId: String): Boolean {
        val file = File(getThumbnailFolder(context), "$videoId.jpg")
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
        
        try {
            val thumbnailFile = File(getThumbnailFolder(context), "$videoId.jpg")
            
            if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                return@withContext true
            }
            
            val url = URL(thumbnailUrl)
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
            
            Log.d(TAG, "Cached thumbnail for video: $videoId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache thumbnail for $videoId: ${e.message}")
            false
        }
    }
    
    /**
     * Get the effective thumbnail URL (local if cached, remote otherwise)
     */
    fun getEffectiveThumbnailUrl(context: Context, videoId: String, remoteUrl: String): String {
        val localPath = getLocalThumbnailPath(context, videoId)
        return if (File(localPath).exists() && File(localPath).length() > 0) {
            "file://$localPath"
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
            Log.d(TAG, "Cleared all cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }
    
    /**
     * Check if cache exists
     */
    fun hasCachedData(context: Context): Boolean {
        return File(context.filesDir, VIDEOS_CACHE_FILE).exists()
    }
}
