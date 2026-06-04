package com.hp.hp_omnipad.ui.home.model

import com.google.firebase.firestore.PropertyName

data class Hero(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val order: Int = 0,
    val active: Boolean = true,
    
    // Timestamp for incremental sync
    val addedAt: Long = 0L,
    val createdAt: Long = 0L,   // exists in Firebase
    val updatedAt: Long = 0L,   // exists in Firebase, used for incrementa

    @get:PropertyName("isYoutubeLink")
    @set:PropertyName("isYoutubeLink")
    var isYoutubeLink: Boolean = false,

    @get:PropertyName("youtubelink")
    @set:PropertyName("youtubelink")
    var youtubelink: String = ""
)
