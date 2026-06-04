package com.hp.hp_omnipad.data.local.dao

import androidx.room.*
import com.hp.hp_omnipad.data.local.entity.HeroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroDao {

    @Query("SELECT * FROM heroes ORDER BY `order` ASC")
    suspend fun getAll(): List<HeroEntity>

    @Query("SELECT COALESCE(MAX(updatedAt), 0) FROM heroes")
    suspend fun getMaxUpdatedAt(): Long

    @Query("SELECT * FROM heroes WHERE active = 1 ORDER BY `order` ASC")
    fun observeActiveHeroes(): Flow<List<HeroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(heroes: List<HeroEntity>)

    // Remove hero that became active=false in delta
    @Query("DELETE FROM heroes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM heroes")
    suspend fun deleteAll()
}
