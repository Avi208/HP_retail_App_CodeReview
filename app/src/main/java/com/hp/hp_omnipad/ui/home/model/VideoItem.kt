package com.hp.hp_omnipad.ui.home.model

data class VideoItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val duration: String = "",
    val views: String = "",
    val categoryIds: List<String> = emptyList(),
    val resources: List<Resource> = emptyList(),
    val isOffline: Boolean = false
)