package com.hp.hp_omnipad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hp.hp_omnipad.data.remote.model.VideoDto

/**
 * Room entity mirroring the Firestore 'videos' collection.
 * categoryIds is stored as a JSON string because Room cannot store List<String> natively.
 * Gson is already in the project dependencies so no extra library is needed.
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationSec: Long,
    val viewCount: Long,
    val published: Boolean,
    val categoryIdsJson: String,  // e.g. ["laptops","gaming"]
    val language: String,
    val updatedAt: Long = 0L,
    val lastFetched: Long = System.currentTimeMillis()
) {
    fun toVideoDto(): VideoDto {
        val type = object : TypeToken<List<String>>() {}.type
        val categoryIds: List<String> = try {
            Gson().fromJson(categoryIdsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return VideoDto(
            id           = id,
            title        = title,
            description  = description,
            videoUrl     = videoUrl,
            thumbnailUrl = thumbnailUrl,
            durationSec  = durationSec,
            viewCount    = viewCount,
            published    = published,
            categoryIds  = categoryIds,
            language     = language,
            updatedAt    = updatedAt
        )
    }
}

fun VideoDto.toVideoEntity(): VideoEntity = VideoEntity(
    id              = id,
    title           = title,
    description     = description,
    videoUrl        = videoUrl,
    thumbnailUrl    = thumbnailUrl ?: "",
    durationSec     = durationSec,
    viewCount       = viewCount,
    published       = published,
    categoryIdsJson = Gson().toJson(categoryIds),
    language        = language ?: "",
    updatedAt       = updatedAt,
    lastFetched     = System.currentTimeMillis()
)
