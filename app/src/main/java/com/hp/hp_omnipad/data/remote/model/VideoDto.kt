package com.hp.hp_omnipad.data.remote.model

import com.hp.hp_omnipad.ui.home.model.Resource

data class VideoDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val durationSec: Long = 0L,
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val storagePath: String = "",
    val addedAt: Long = 0L,
    val published: Boolean = false,
    val viewCount: Long = 0L,
    val language: String = "",
    val relatedIds: List<String> = emptyList(),
    val updatedAt: Long = 0L,

    // NEW FIELD (maps Firestore resources array)
    val resources: List<Resource> = emptyList()
)