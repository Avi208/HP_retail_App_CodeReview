package com.hp.hp_omnipad.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hp.hp_omnipad.data.local.dao.CategoryDao
import com.hp.hp_omnipad.data.local.dao.HeroDao
import com.hp.hp_omnipad.data.local.dao.VideoDao
import com.hp.hp_omnipad.data.local.entity.CategoryEntity
import com.hp.hp_omnipad.data.local.entity.HeroEntity
import com.hp.hp_omnipad.data.local.entity.VideoEntity

/**
 * Single Room database for HP OmniPad.
 *
 * Stored at: /data/data/com.hp.hp_omnipad/databases/omnipad.db
 * This path survives "Clear Cache" — only "Clear Data" wipes it.
 *
 * Bump [version] and add a Migration whenever you add/remove/rename columns.
 * fallbackToDestructiveMigration() is safe here because this is a pure cache —
 * all data can be re-fetched from Firebase.
 */
@Database(
    entities = [
        HeroEntity::class,
        VideoEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun heroDao(): HeroDao
    abstract fun videoDao(): VideoDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnipad.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
