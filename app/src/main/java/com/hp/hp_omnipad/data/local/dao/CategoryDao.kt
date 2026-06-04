package com.hp.hp_omnipad.data.local.dao

import androidx.room.*
import com.hp.hp_omnipad.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COALESCE(MAX(updatedAt), 0) FROM categories")
    suspend fun getMaxUpdatedAt(): Long

    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}