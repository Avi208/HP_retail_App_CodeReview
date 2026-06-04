package com.hp.hp_omnipad.data.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.hp.hp_omnipad.data.local.AppDatabase
import com.hp.hp_omnipad.data.local.entity.toCategoryEntity
import com.hp.hp_omnipad.data.local.entity.toVideoEntity
import com.hp.hp_omnipad.data.remote.model.CategoryDto
import com.hp.hp_omnipad.data.remote.model.VideoDto
import com.hp.hp_omnipad.ui.home.model.VideoItem
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private const val TAG = "FirestoreRepository"
    private const val PREFS_NAME = "sync_prefs"
    private const val KEY_LAST_FULL_VIDEO_SYNC = "last_full_video_sync"
    private const val KEY_LAST_FULL_CAT_SYNC   = "last_full_cat_sync"
    private const val FULL_SYNC_INTERVAL_MS     = 24 * 60 * 60 * 1000L // 24 h

    private val firestore = FirebaseFirestore.getInstance()

    private var cachedCategories: List<CategoryDto>? = null
    private var cachedVideos: List<VideoDto>? = null

    // Track if memory cache was seeded by JSON (less reliable than Room)
    private var isMemorySeededByJson = false

    private var room: AppDatabase? = null
    private var appContext: Context? = null

    fun initRoom(context: Context) {
        appContext = context.applicationContext
        if (room == null) {
            room = AppDatabase.getInstance(context)
            Log.d(TAG, "Room initialised")
        }
    }

    // ── SharedPrefs helpers ──────────────────────────────────────────────────
    private fun getLastFullVideoSync() = appContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?.getLong(KEY_LAST_FULL_VIDEO_SYNC, 0L) ?: 0L

    private fun getLastFullCatSync() = appContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?.getLong(KEY_LAST_FULL_CAT_SYNC, 0L) ?: 0L

    private fun saveLastFullVideoSync(ts: Long) = appContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?.edit { putLong(KEY_LAST_FULL_VIDEO_SYNC, ts) }

    private fun saveLastFullCatSync(ts: Long) = appContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?.edit { putLong(KEY_LAST_FULL_CAT_SYNC, ts) }

    // ── Public cache seed (called from SplashViewModel with bundled JSON) ───
    fun setLocalCache(categories: List<CategoryDto>, videos: List<VideoDto>) {
        cachedCategories = categories
        cachedVideos = videos
        isMemorySeededByJson = true
    }

    // ── Categories ───────────────────────────────────────────────────────────
    suspend fun getCategories(): List<CategoryDto> {
        if (cachedCategories == null || isMemorySeededByJson) {
            val roomData = room?.categoryDao()?.getAll()?.map { it.toCategoryDto() }
            if (!roomData.isNullOrEmpty()) {
                cachedCategories = roomData
                isMemorySeededByJson = false
                return roomData
            }
        } else {
            return cachedCategories!!
        }
        return fetchFromRemote()
    }

    suspend fun fetchFromRemote(): List<CategoryDto> {
        val maxUpdatedAt = room?.categoryDao()?.getMaxUpdatedAt() ?: 0L
        val lastFullSync = getLastFullCatSync()
        val needsFullSync = maxUpdatedAt == 0L ||
                (System.currentTimeMillis() - lastFullSync) > FULL_SYNC_INTERVAL_MS

        Log.d(TAG, "Category sync — maxUpdatedAt=$maxUpdatedAt, fullSync=$needsFullSync")

        try {
            val query = if (needsFullSync) {
                firestore.collection("categories")
            } else {
                firestore.collection("categories")
                    .whereGreaterThan("updatedAt", maxUpdatedAt)
            }

            val snapshot = query.get().await()
            Log.d(TAG, "Firebase returned ${snapshot.size()} category docs")

            if (snapshot.isEmpty && !needsFullSync) {
                // Nothing changed — return what we have
                return cachedCategories
                    ?: room?.categoryDao()?.getAll()?.map { it.toCategoryDto() }
                    ?: emptyList()
            }

            val fetched = snapshot.documents
                .mapNotNull { it.toObject<CategoryDto>()?.copy(id = it.id) }

            if (fetched.isNotEmpty()) {
                if (needsFullSync) {
                    room?.categoryDao()?.deleteAll()
                    room?.categoryDao()?.insertAll(fetched.map { it.toCategoryEntity() })
                    saveLastFullCatSync(System.currentTimeMillis())
                } else {
                    // Incremental upsert — categories have no "active" flag so just upsert
                    room?.categoryDao()?.insertAll(fetched.map { it.toCategoryEntity() })
                }
            }

            val updated = room?.categoryDao()?.getAll()?.map { it.toCategoryDto() } ?: emptyList()
            cachedCategories = updated
            isMemorySeededByJson = false
            return updated

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories: ${e.message}")
            return cachedCategories ?: emptyList()
        }
    }

    // ── Videos ───────────────────────────────────────────────────────────────
    suspend fun getVideos(): List<VideoDto> {
        if (cachedVideos == null || isMemorySeededByJson) {
            val roomData = room?.videoDao()?.getAll()?.map { it.toVideoDto() }
            if (!roomData.isNullOrEmpty()) {
                cachedVideos = roomData
                isMemorySeededByJson = false
                return roomData
            }
        } else {
            return cachedVideos!!
        }
        return fetchVideosFromRemote()
    }

    suspend fun fetchVideosFromRemote(): List<VideoDto> {
        val maxUpdatedAt = room?.videoDao()?.getMaxUpdatedAt() ?: 0L
        val lastFullSync = getLastFullVideoSync()
        val needsFullSync = maxUpdatedAt == 0L ||
                (System.currentTimeMillis() - lastFullSync) > FULL_SYNC_INTERVAL_MS

        Log.d(TAG, "Video sync — maxUpdatedAt=$maxUpdatedAt, fullSync=$needsFullSync")

        try {
            val query = if (needsFullSync) {
                firestore.collection("videos")
            } else {
                firestore.collection("videos")
                    .whereGreaterThan("updatedAt", maxUpdatedAt)
            }

            val snapshot = query.get().await()
            Log.d(TAG, "Firebase returned ${snapshot.size()} video docs")

            if (snapshot.isEmpty && !needsFullSync) {
                // Nothing changed since last sync
                return cachedVideos
                    ?: room?.videoDao()?.getAll()?.map { it.toVideoDto() }
                    ?: emptyList()
            }

            val fetched = snapshot.documents.mapNotNull { doc ->
                doc.toObject<VideoDto>()?.copy(
                    id           = doc.id,
                    thumbnailUrl = convertDriveUrl(doc.getString("thumbnailUrl") ?: "")
                )
            }

            if (needsFullSync && fetched.isNotEmpty()) {
                // Full sync: replace everything, keep only published
                room?.videoDao()?.deleteAll()
                room?.videoDao()?.insertAll(
                    fetched.filter { it.published }.map { it.toVideoEntity() }
                )
                saveLastFullVideoSync(System.currentTimeMillis())
            } else if (fetched.isNotEmpty()) {
                // Incremental: upsert published, remove newly-unpublished
                val (published, unpublished) = fetched.partition { it.published }
                if (published.isNotEmpty()) {
                    room?.videoDao()?.insertAll(published.map { it.toVideoEntity() })
                }
                unpublished.forEach { room?.videoDao()?.deleteById(it.id) }
            }

            val updated = room?.videoDao()?.getAll()?.map { it.toVideoDto() } ?: emptyList()
            cachedVideos = updated
            isMemorySeededByJson = false
            return updated

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching videos: ${e.message}")
            return cachedVideos ?: emptyList()
        }
    }

    // ── Misc Firebase ops ────────────────────────────────────────────────────
    suspend fun incrementViewCount(videoId: String) {
        try {
            firestore.collection("videos").document(videoId)
                .update("viewCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment view count")
        }
    }

    fun clearCache() {
        cachedCategories = null
        cachedVideos = null
        isMemorySeededByJson = false
    }

    suspend fun clearRoomCache() {
        room?.categoryDao()?.deleteAll()
        room?.videoDao()?.deleteAll()
        // Reset full-sync timestamps so next launch does a fresh full sync
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit { clear() }
        cachedCategories = null
        cachedVideos = null
    }

    private fun convertDriveUrl(url: String): String {
        if (url.isEmpty()) return url
        val fileDRegex = "/file/d/([^/]+)".toRegex()
        val ucIdRegex  = "id=([^&]+)".toRegex()
        val id = fileDRegex.find(url)?.groupValues?.get(1)
            ?: ucIdRegex.find(url)?.groupValues?.get(1)
        return if (id != null) "https://drive.google.com/thumbnail?id=$id&sz=w400" else url
    }

    // ── Downloaded videos (unchanged) ────────────────────────────────────────
    suspend fun saveDownloadedVideo(
        videoId: String, title: String, localPath: String,
        thumbnailUrl: String, downloadId: Long
    ) {
        val data = hashMapOf(
            "videoId"      to videoId,
            "title"        to title,
            "localPath"    to localPath,
            "thumbnailUrl" to thumbnailUrl,
            "downloadedAt" to System.currentTimeMillis()
        )
        firestore.collection("downloaded_videos").document(videoId).set(data).await()
    }

    suspend fun getDownloadedVideos(): List<VideoItem> {
        return try {
            val snapshot = firestore.collection("downloaded_videos").get().await()
            snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val path  = doc.getString("localPath") ?: return@mapNotNull null
                val thumb = doc.getString("thumbnailUrl") ?: ""
                VideoItem(
                    id = doc.id, title = title, description = "", videoUrl = path,
                    thumbnailUrl = thumb, duration = "", views = "",
                    categoryIds = emptyList(), resources = emptyList()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteDownloadedVideo(videoId: String) {
        try {
            firestore.collection("downloaded_videos").document(videoId).delete().await()
            Log.d(TAG, "Deleted downloaded video record for: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete downloaded video record: ${e.message}")
        }
    }
}