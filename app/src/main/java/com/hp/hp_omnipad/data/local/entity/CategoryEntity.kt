package com.hp.hp_omnipad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hp.hp_omnipad.data.remote.model.CategoryDto

/**
 * Room entity mirroring the Firestore 'categories' collection.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val order: Long,
    val updatedAt: Long = 0L,
    val lastFetched: Long = System.currentTimeMillis()
) {
    fun toCategoryDto(): CategoryDto = CategoryDto(
        id    = id,
        name  = name,
        slug  = slug,
        order = order,
        updatedAt = updatedAt
    )
}

fun CategoryDto.toCategoryEntity(): CategoryEntity = CategoryEntity(
    id          = id,
    name        = name  ?: "",
    slug        = slug  ?: "",
    order       = order,
    updatedAt   = updatedAt,
    lastFetched = System.currentTimeMillis()
)
