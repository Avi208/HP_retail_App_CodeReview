package com.hp.hp_omnipad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hp.hp_omnipad.ui.home.model.Hero

/**
 * Room entity mirroring the Firestore 'heroes' collection.
 * Survives app kill AND Clear Cache. Only wiped by Clear Data.
 */
@Entity(tableName = "heroes")
data class HeroEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val active: Boolean,
    val order: Int,
    val isYoutubeLink: Boolean,
    val youtubelink: String,
    val updatedAt: Long = 0L,
    val lastFetched: Long = System.currentTimeMillis()
) {
    fun toHero(): Hero = Hero(
        id            = id,
        title         = title,
        videoUrl      = videoUrl,
        thumbnailUrl  = thumbnailUrl,
        active        = active,
        order         = order,
        isYoutubeLink = isYoutubeLink,
        youtubelink   = youtubelink,
        updatedAt   = updatedAt,
    )
}

fun Hero.toHeroEntity(): HeroEntity = HeroEntity(
    id            = id,
    title         = title,
    videoUrl      = videoUrl,
    thumbnailUrl  = thumbnailUrl,
    active        = active,
    order         = order,
    isYoutubeLink = isYoutubeLink,
    youtubelink   = youtubelink,
    updatedAt     = updatedAt,
    lastFetched   = System.currentTimeMillis()
)
