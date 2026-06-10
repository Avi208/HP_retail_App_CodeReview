package com.hp.hp_omnipad.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SyncState(
    val isSyncing: Boolean = false,
    val message: String = "",
    val progress: Float = 0f,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val currentFileProgress: Float = 0f, // Progress of the current file being downloaded (0.0 to 1.0)
    val successfulDownloads: Int = 0,
    val isDownloadingVideos: Boolean = false,
    val hasStartedDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val currentDownloadTitle: String = ""
)

object SyncManager {
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState = _syncState.asStateFlow()

    
    fun startSync(message: String, totalItems: Int = 0, isDownloadingVideos: Boolean = false) {
        _syncState.value = SyncState(
            isSyncing = true,
            message = message,
            totalItems = totalItems,
            completedItems = 0,
            progress = 0f,
            isDownloadingVideos = isDownloadingVideos,
            hasStartedDownloading = true,
            isCompleted = false
        )
    }

    fun updateCurrentTitle(title: String) {
        _syncState.value = _syncState.value.copy(currentDownloadTitle = title)
    }
    
    /**
     * Updates the progress of the currently downloading file.
     * Calculated as: (completedItems + currentFileProgress) / totalItems
     */
    /* fun updateFileProgress(fileProgress: Float) {
        val current = _syncState.value
        if (current.totalItems <= 0) return

        val normalizedFileProgress = fileProgress.coerceIn(0f, 1f)
        val overallProgress = (current.completedItems.toFloat() + normalizedFileProgress) / current.totalItems.toFloat()
        
        _syncState.value = current.copy(
            currentFileProgress = normalizedFileProgress,
            progress = overallProgress.coerceIn(0f, 1f)
        )
    }
    
    fun updateProgress(completedItems: Int, message: String? = null) {
        val current = _syncState.value
        val progress = if (current.totalItems > 0) {
            completedItems.toFloat() / current.totalItems.toFloat()
        } else 0f
        
        Log.d(TAG, "Progress: $completedItems/${current.totalItems} (${(progress * 100).toInt()}%)")
        
        _syncState.value = current.copy(
            completedItems = completedItems,
            currentFileProgress = 0f, // Reset file progress when an item completes
            progress = progress.coerceIn(0f, 1f),
            message = message ?: current.message,
            hasStartedDownloading = true
        )
    }*/

    fun updateFileProgress(fileProgress: Float) {
        _syncState.update { current ->
            if (current.totalItems <= 0) return@update current
            val normalizedFileProgress = fileProgress.coerceIn(0f, 1f)
            val overallProgress = (current.completedItems + normalizedFileProgress) / current.totalItems.toFloat()
            current.copy(
                currentFileProgress = normalizedFileProgress,
                progress = overallProgress.coerceIn(0f, 1f)
            )
        }
    }

    fun updateProgress(completedItems: Int, message: String? = null) {
        _syncState.update { current ->
            val progress = if (current.totalItems > 0)
                completedItems.toFloat() / current.totalItems.toFloat() else 0f
            current.copy(
                completedItems = completedItems,
                currentFileProgress = 0f,
                currentDownloadTitle = "",
                progress = progress.coerceIn(0f, 1f),
                message = message ?: current.message,
                hasStartedDownloading = true
            )
        }
    }
    
    fun updateMessage(message: String) {
        _syncState.value = _syncState.value.copy(message = message)
    }
    
    fun incrementSuccessfulDownloads() {
        val current = _syncState.value
        _syncState.value = current.copy(
            successfulDownloads = current.successfulDownloads + 1
        )
    }
    
    fun completeSync(successCount: Int = 0, skipCount: Int = 0, failCount: Int = 0) {
        val current = _syncState.value
        
        val hasContent = successCount > 0 || skipCount > 0
        val allFailed = failCount > 0 && successCount == 0 && skipCount == 0
        
        _syncState.value = current.copy(
            isSyncing = false,
            message = when {
                allFailed -> "Download failed"
                hasContent -> "Ready for offline"
                else -> ""
            },
            progress = 1f,
            completedItems = current.totalItems,
            currentFileProgress = 0f,
            successfulDownloads = successCount,
            isCompleted = hasContent && !allFailed
        )
    }
    
    fun reset() {
        _syncState.value = SyncState()
    }
}
