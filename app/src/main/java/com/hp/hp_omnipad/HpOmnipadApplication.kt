package com.hp.hp_omnipad

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.hp.hp_omnipad.utils.FileDownloader
import com.hp.hp_omnipad.utils.RealtimeSyncService
import com.hp.hp_omnipad.utils.VideoSyncManager

class HpOmnipadApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Initialize download managers with application context
        VideoSyncManager.initialize(this)
        FileDownloader.initialize(this)

        // Initialize sync service ONLY (do NOT start it here)
        // Actual start happens in SplashViewModel after Firebase fetch
        RealtimeSyncService.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // 30% RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.10) // 10% disk
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}