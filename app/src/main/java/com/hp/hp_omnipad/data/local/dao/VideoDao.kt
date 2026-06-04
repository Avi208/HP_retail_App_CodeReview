package com.hp.hp_omnipad.data.local.dao

import androidx.room.*
import com.hp.hp_omnipad.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    // One-shot reads (used by repositories / splash)
    @Query("SELECT * FROM videos ORDER BY lastFetched DESC")
    suspend fun getAll(): List<VideoEntity>

    // ── Delta sync cursor ─
    // Returns the highest updatedAt stored in Room.
    // 0L when Room is empty → triggers full fetch.
    @Query("SELECT COALESCE(MAX(updatedAt), 0) FROM videos")
    suspend fun getMaxUpdatedAt(): Long

    // Flow for UI (cache-first: Room pushes on every insertAll)
    @Query("SELECT * FROM videos ORDER BY lastFetched DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE categoryIdsJson LIKE '%' || :categoryId || '%' ORDER BY lastFetched DESC")
    fun observeByCategory(categoryId: String): Flow<List<VideoEntity>>

    // Upsert — INSERT OR REPLACE on primary key conflict
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    // Soft delete — remove a video that became published=false in delta
    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM videos")
    suspend fun deleteAll()
}