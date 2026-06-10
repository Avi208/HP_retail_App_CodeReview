package com.hp.hp_omnipad.ui.home.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hp.hp_omnipad.data.repository.FirestoreRepository
import com.hp.hp_omnipad.data.repository.HeroRepository
import com.hp.hp_omnipad.ui.home.model.Category
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.utils.SyncManager
import com.hp.hp_omnipad.utils.SyncState
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // Expose sync state directly so the UI can observe progress percentage
    val syncState: StateFlow<SyncState> = SyncManager.syncState

    private var isFetching = false

    /* init {
        loadFromCache()
        loadOfflineVideos()
        
        // Observe SyncManager to update offline video count dynamically
        viewModelScope.launch {
            var lastCompletedCount = -1
            SyncManager.syncState.collectLatest { state ->
                val syncJustFinished = !state.isSyncing && state.isCompleted
                val newItemCompleted = state.completedItems != lastCompletedCount && state.isSyncing
                
                // When a sync completes or a new item is updated, refresh the offline list
                if (syncJustFinished || newItemCompleted) {
                    lastCompletedCount = state.completedItems
                    loadOfflineVideosInternal()
                }
            }
        }
    }*/

    init {
        loadFromCache()
        loadOfflineVideos()

        viewModelScope.launch {
            var lastCompletedCount = -1
            SyncManager.syncState.collect { state ->   // ← collect, not collectLatest
                val syncJustFinished = !state.isSyncing && state.isCompleted
                val newItemCompleted = state.completedItems != lastCompletedCount && state.isSyncing

                if (syncJustFinished || newItemCompleted) {
                    lastCompletedCount = state.completedItems
                    loadOfflineVideosInternal()        // ← now allowed to finish
                }
            }
        }
    }

    /**
     * Load videos that have been downloaded for offline use.
     */
    private fun loadOfflineVideos() {
        viewModelScope.launch {
            loadOfflineVideosInternal()
        }
    }

    private suspend fun loadOfflineVideosInternal() = withContext(Dispatchers.IO) {
        try {
            val downloadedVideos = VideoSyncManager.getAllDownloadedVideos()
            val offlineVideoItems = downloadedVideos.map { downloaded ->
                VideoItem(
                    id           = downloaded.id,
                    title        = downloaded.title,
                    description  = downloaded.description,
                    videoUrl     = VideoSyncManager.getLocalVideoPath(downloaded.id) ?: "",
                    thumbnailUrl = VideoSyncManager.getLocalThumbnailPath(downloaded.id) ?: "",
                    duration     = downloaded.duration,
                    views        = "Offline",
                    categoryIds  = downloaded.categoryIds,
                    resources    = emptyList(),
                    isOffline    = true
                )
            }
            
            withContext(Dispatchers.Main) {
                // Remove the size-only guard to ensure UI always reflects real disk state
                _uiState.value = _uiState.value.copy(offlineVideos = offlineVideoItems)
            }
        } catch (e: Exception) {
        }
    }

    fun refreshOfflineVideos() {
        loadOfflineVideos()
    }

    /**
     * Initial load — uses Room/memory cache first.
     */
    private fun loadFromCache() {
        viewModelScope.launch {
            try {
                val categoryDtos = FirestoreRepository.getCategories()
                val videoDtos    = FirestoreRepository.getVideos()
                updateUiState(categoryDtos, videoDtos)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * User-triggered refresh.
     */
    fun refresh() {
        if (isFetching) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            FirestoreRepository.clearCache()
            HeroRepository.clearCache()

            fetchDataFromRemote()
            checkForDeletedVideos()

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    /**
     * Force-fetch directly from Firebase.
     */
    private suspend fun fetchDataFromRemote() {
        isFetching = true
        try {
            val categoryDtos = FirestoreRepository.fetchFromRemote()
            val videoDtos    = FirestoreRepository.fetchVideosFromRemote()
            val heroDtos     = HeroRepository.fetchFromRemote()
            
            updateUiState(categoryDtos, videoDtos)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
        } finally {
            isFetching = false
        }
    }

    /**
     * Check if any locally downloaded videos were removed from Firebase.
     */
    private suspend fun checkForDeletedVideos() = withContext(Dispatchers.IO) {
        try {
            val videoDtos = FirestoreRepository.getVideos()
            val heroes    = HeroRepository.getHeroes()
            VideoSyncManager.checkAndRemoveDeletedVideos(getApplication(), videoDtos, heroes)
            loadOfflineVideosInternal()
        } catch (e: Exception) {
        }
    }

    /**
     * Shared helper to map DTOs → UI state.
     */
    private fun updateUiState(
        categoryDtos: List<com.hp.hp_omnipad.data.remote.model.CategoryDto>,
        videoDtos: List<com.hp.hp_omnipad.data.remote.model.VideoDto>
    ) {
        val categories = categoryDtos.map { dto ->
            val count = videoDtos.count { video -> video.categoryIds.contains(dto.slug) }
            Category(
                title      = dto.name,
                subtitle   = "",
                videoCount = count,
                icon       = mapIconFromSlug(dto.slug),
                slug       = dto.slug
            )
        }
        val videos = videoDtos.map { dto ->
            VideoItem(
                id           = dto.id,
                title        = dto.title,
                description  = dto.description,
                videoUrl     = dto.videoUrl,
                thumbnailUrl = dto.thumbnailUrl ?: "",
                duration     = formatDuration(dto.durationSec),
                views        = "${dto.viewCount} views",
                categoryIds  = dto.categoryIds,
                resources    = dto.resources ?: emptyList()
            )
        }
        _uiState.value = _uiState.value.copy(
            categories    = categories,
            allVideos     = videos,
            recentlyAdded = videos.take(3),
            isLoading     = false,
            error         = null
        )
    }

    private fun mapIconFromSlug(slug: String) =
        when (slug) {
            "demos"       -> Icons.Default.PlayCircle
            "training"    -> Icons.Default.School
            "promotional" -> Icons.Default.Campaign
            "events"      -> Icons.Default.Event
            else          -> Icons.Default.PlayCircle
        }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun getVideoById(id: String?): VideoItem? =
        _uiState.value.allVideos.firstOrNull { it.id == id }

    fun incrementView(videoId: String) {
        viewModelScope.launch {
            try {
                FirestoreRepository.incrementViewCount(videoId)
                val updateViewCount: (VideoItem) -> VideoItem = { video ->
                    if (video.id == videoId) {
                        val current = video.views.replace(" views", "").toLongOrNull() ?: 0
                        video.copy(views = "${current + 1} views")
                    } else video
                }
                _uiState.value = _uiState.value.copy(
                    allVideos     = _uiState.value.allVideos.map(updateViewCount),
                    recentlyAdded = _uiState.value.recentlyAdded.map(updateViewCount)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
