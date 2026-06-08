package com.hp.hp_omnipad.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.hp.hp_omnipad.data.remote.model.VideoDto
import com.hp.hp_omnipad.data.repository.FirestoreRepository
import com.hp.hp_omnipad.data.repository.HeroRepository
import com.hp.hp_omnipad.ui.home.model.Hero
import com.hp.hp_omnipad.ui.home.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages background sync between Firebase and local storage.
 */
object RealtimeSyncService {

    private const val TAG = "RealtimeSyncService"
    private val db = FirebaseFirestore.getInstance()

    private var appContext: Context? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed class SyncEvent {
        data class VideoAdded(val videoId: String, val title: String) : SyncEvent()
        data class VideoRemoved(val videoId: String, val title: String) : SyncEvent()
        data class HeroAdded(val heroId: String, val title: String) : SyncEvent()
        data class HeroRemoved(val heroId: String, val title: String) : SyncEvent()
        object DataChanged : SyncEvent()
    }

    private val _syncEvents = MutableSharedFlow<SyncEvent>()
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "RealtimeSyncService initialized")
    }

    /**
     * Called from SplashViewModel AFTER it has already fetched fresh data from Firebase.
     */
    fun startListeningWithKnownData(
        publishedVideoIds: Set<String>,
        activeHeroIds: Set<String>,
        context: Context
    ) {
        if (_isListening.value) {
            Log.d(TAG, "Already running — skipping")
            return
        }

        appContext = context.applicationContext
        Log.d(TAG, "Starting sync with pre-fetched data (no duplicate Firebase call)")

        // Run local cleanup using already-known IDs
        serviceScope.launch {
            cleanupWithKnownData(publishedVideoIds, activeHeroIds)
        }

        // Start periodic background refresh
        startPeriodicRefresh()

        _isListening.value = true
    }

    fun stopListening() {
        Log.d(TAG, "Stopping sync service")
        _isListening.value = false
    }

    /**
     * Hourly background refresh loop.
     */
    private fun startPeriodicRefresh() {
        serviceScope.launch {
            while (_isListening.value) {
                delay(60 * 60 * 1000L) // 1 hour
                if (!_isListening.value) break

                try {
                    Log.d(TAG, "Hourly refresh — checking for changes")

                    val videos = FirestoreRepository.getVideos()
                    val heroes = HeroRepository.getHeroes()

                    val publishedVideoIds = videos.map { it.id }.toSet()
                    val activeHeroIds     = heroes.map { it.id }.toSet()

                    val ctx = appContext ?: continue
                    if (SettingsViewModel.isOffloadEnabled(ctx)) {
                        videos.forEach { video ->
                            if (!VideoSyncManager.isVideoDownloaded(video.id)) {
                                handleVideoAdded(video)
                            }
                        }
                        heroes.forEach { hero ->
                            if (!VideoSyncManager.isVideoDownloaded(hero.id)) {
                                handleHeroAdded(hero)
                            }
                        }
                    }

                    cleanupWithKnownData(publishedVideoIds, activeHeroIds)

                    _syncEvents.emit(SyncEvent.DataChanged)
                    Log.d(TAG, "Hourly refresh complete")

                } catch (e: Exception) {
                    SafeLog.e(TAG, "Hourly refresh error: %s", e.message)
                }
            }
        }
    }

    /**
     * Deletes local videos that are no longer published/active in Firebase.
     */
    suspend fun cleanupWithKnownData(
        publishedVideoIds: Set<String>,
        activeHeroIds: Set<String>
    ) {
        val context = appContext ?: return
        val validIds = publishedVideoIds + activeHeroIds

        VideoSyncManager.initialize(context)
        val localVideoIds = VideoSyncManager.getLocalVideoIds()
        val toDelete = localVideoIds.filter { it !in validIds }

        if (toDelete.isNotEmpty()) {
            Log.d(TAG, "Removing ${toDelete.size} unpublished/inactive local videos")
            for (videoId in toDelete) {
                val folder = VideoSyncManager.findVideoFolder(videoId)
                if (folder != null && folder.exists()) {
                    val title = folder.name
                    folder.deleteRecursively()
                    SafeLog.d(TAG, "Deleted: %s", title)
                    _syncEvents.emit(SyncEvent.VideoRemoved(videoId, title))
                }
            }
            _syncEvents.emit(SyncEvent.DataChanged)
        } else {
            Log.d(TAG, "No local videos to clean up")
        }
    }

    private fun handleVideoAdded(video: VideoDto) {
        val context = appContext ?: return
        if (!SettingsViewModel.isOffloadEnabled(context)) return
        if (VideoSyncManager.isVideoDownloaded(video.id)) return

        serviceScope.launch {
            try {
                SafeLog.d(TAG, "Auto-downloading new video: %s", video.title)
                SyncManager.startSync("Downloading: ${video.title}", 1, isDownloadingVideos = true)
                VideoSyncManager.initialize(context)
                val success = downloadVideo(video)
                if (success) {
                    SyncManager.completeSync(1, 0, 0)
                    _syncEvents.emit(SyncEvent.VideoAdded(video.id, video.title))
                    _syncEvents.emit(SyncEvent.DataChanged)
                } else {
                    SyncManager.completeSync(0, 0, 1)
                }
            } catch (e: Exception) {
                SafeLog.e(TAG, "Error auto-downloading video: %s", e.message)
            }
        }
    }

    private fun handleHeroAdded(hero: Hero) {
        val context = appContext ?: return
        if (!SettingsViewModel.isOffloadEnabled(context)) return
        if (VideoSyncManager.isVideoDownloaded(hero.id)) return

        serviceScope.launch {
            try {
                SafeLog.d(TAG, "Auto-downloading new hero: %s", hero.title)
                SyncManager.startSync("Downloading: ${hero.title}", 1, isDownloadingVideos = true)
                VideoSyncManager.initialize(context)
                val success = downloadHero(hero)
                if (success) {
                    SyncManager.completeSync(1, 0, 0)
                    _syncEvents.emit(SyncEvent.HeroAdded(hero.id, hero.title))
                    _syncEvents.emit(SyncEvent.DataChanged)
                } else {
                    SyncManager.completeSync(0, 0, 1)
                }
            } catch (e: Exception) {
                SafeLog.e(TAG, "Error auto-downloading hero: %s", e.message)
            }
        }
    }

    private suspend fun downloadVideo(video: VideoDto): Boolean {
        return try {
            VideoSyncManager.downloadSingleVideo(
                videoId      = video.id,
                videoUrl     = video.videoUrl,
                title        = video.title,
                description  = video.description,
                thumbnailUrl = video.thumbnailUrl ?: "",
                duration     = formatDuration(video.durationSec),
                categoryIds  = video.categoryIds,
                isHero       = false
            )
        } catch (e: Exception) {
            SafeLog.e(TAG, "Download error: %s", e.message)
            false
        }
    }

    private suspend fun downloadHero(hero: Hero): Boolean {
        return try {
            VideoSyncManager.downloadSingleVideo(
                videoId      = hero.id,
                videoUrl     = hero.videoUrl,
                title        = hero.title,
                description  = "",
                thumbnailUrl = hero.thumbnailUrl,
                duration     = "",
                categoryIds  = emptyList(),
                isHero       = true
            )
        } catch (e: Exception) {
            SafeLog.e(TAG, "Download error: %s", e.message)
            false
        }
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }
}
