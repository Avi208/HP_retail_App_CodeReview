package com.hp.hp_omnipad.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.hp.hp_omnipad.data.local.AppDatabase
import com.hp.hp_omnipad.data.local.entity.toHeroEntity
import com.hp.hp_omnipad.ui.home.model.Hero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object HeroRepository {

    private const val TAG = "HeroRepository"

    private val db = FirebaseFirestore.getInstance()

    // -------- In-memory cache & Flow --------
    private var cachedHeroes: List<Hero>? = null
    private val _heroesFlow = MutableStateFlow<List<Hero>>(emptyList())
    val heroesFlow = _heroesFlow.asStateFlow()

    // -------- Context & Room --------
    private var room: AppDatabase? = null
    private var appContext: Context? = null

    fun initRoom(context: Context) {
        appContext = context.applicationContext
        if (room == null) {
            room = AppDatabase.getInstance(context)
            Log.d(TAG, "Room initialised")
        }
    }

    /**
     * setLocalCache is deprecated to avoid "Fighting Caches" glitch.
     * Room is the single source of truth for persistent data.
     */
    fun setLocalCache(heroes: List<Hero>) {
        Log.d(TAG, "setLocalCache ignored - using Room instead")
    }

    suspend fun getHeroes(): List<Hero> {
        // 1. Return in-memory cache if available
        cachedHeroes?.let {
            _heroesFlow.value = it
            return it
        }

        // 2. Load from Room (master source)
        val roomHeroes = room?.heroDao()?.getAll()
        if (!roomHeroes.isNullOrEmpty()) {
            val heroes = roomHeroes.map { it.toHero() }
            cachedHeroes = heroes
            _heroesFlow.value = heroes
            Log.d(TAG, "Heroes loaded from Room database")
            return heroes
        }

        // 3. Fallback to Firebase
        return fetchFromRemote()
    }

    /**
     * Incremental sync:
     * - First launch (Room empty) → full fetch, wipe + replace.
     * - Subsequent launches → only fetch docs with updatedAt > MAX(Room.updatedAt).
     * Heroes change rarely so no periodic full-sync timer is needed.
     */
    suspend fun fetchFromRemote(): List<Hero> {
        val maxUpdatedAt = room?.heroDao()?.getMaxUpdatedAt() ?: 0L
        val needsFullSync = maxUpdatedAt == 0L

        Log.d(TAG, "Hero sync — maxUpdatedAt=$maxUpdatedAt, fullSync=$needsFullSync")

        try {
            val query = if (needsFullSync) {
                db.collection("heroes")
            } else {
                db.collection("heroes").whereGreaterThan("updatedAt", maxUpdatedAt)
            }

            val snapshot = query.get().await()
            Log.d(TAG, "Firebase returned ${snapshot.size()} hero docs")

            if (snapshot.isEmpty && !needsFullSync) {
                // Nothing changed — return existing data
                return cachedHeroes
                    ?: room?.heroDao()?.getAll()?.map { it.toHero() }
                    ?: emptyList()
            }

            val fetched = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Hero::class.java)?.copy(id = doc.id)
            }

            if (needsFullSync) {
                // Full sync: wipe then insert only active heroes
                room?.heroDao()?.deleteAll()
                room?.heroDao()?.insertAll(
                    fetched.filter { it.active }.map { it.toHeroEntity() }
                )
            } else {
                // Incremental: upsert active, remove newly-inactive
                val (active, inactive) = fetched.partition { it.active }
                if (active.isNotEmpty()) {
                    room?.heroDao()?.insertAll(active.map { it.toHeroEntity() })
                }
                inactive.forEach { room?.heroDao()?.deleteById(it.id) }
            }

            val updated = room?.heroDao()?.getAll()?.map { it.toHero() }
                ?.sortedBy { it.order } ?: emptyList()
            cachedHeroes = updated
            _heroesFlow.value = updated
            Log.d(TAG, "Room mirror updated: ${updated.size} heroes")
            return updated

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch heroes: ${e.message}")
            return cachedHeroes ?: emptyList()
        }
    }

    fun clearCache() {
        cachedHeroes = null
    }

    suspend fun clearRoomCache() {
        room?.heroDao()?.deleteAll()
        _heroesFlow.value = emptyList()
        cachedHeroes = null
    }
}