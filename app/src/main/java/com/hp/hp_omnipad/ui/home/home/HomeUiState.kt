package com.hp.hp_omnipad.ui.home.home

import com.hp.hp_omnipad.ui.home.model.Category
import com.hp.hp_omnipad.ui.home.model.VideoItem

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val allVideos: List<VideoItem> = emptyList(),
    val recentlyAdded: List<VideoItem> = emptyList(),
    val offlineVideos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgress: Float = 0f,
    val error: String? = null
)