package com.hp.hp_omnipad.ui.home.model

import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val title: String,
    val subtitle: String,
    val videoCount: Int,
    val icon: ImageVector,
    val slug: String
)