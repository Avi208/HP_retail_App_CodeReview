package com.hp.hp_omnipad.ui.splash

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hp.hp_omnipad.data.repository.FirestoreRepository
import com.hp.hp_omnipad.data.repository.HeroRepository
import com.hp.hp_omnipad.ui.home.settings.SettingsViewModel
import com.hp.hp_omnipad.utils.RealtimeSyncService
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SplashState {
    object Loading : SplashState()
    object NavigateToHome : SplashState()
}

class SplashViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val connectivityManager =
        application.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _uiState = MutableStateFlow<SplashState>(SplashState.Loading)
    val uiState = _uiState.asStateFlow()

    // Separate scope so the video sync continues running after the user
    // navigates away from the splash screen (viewModelScope would cancel it).
    //private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startSplashProcess()
    }

    /*private fun startSplashProcess() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // 1. Initialise Room on a background thread
            withContext(Dispatchers.IO) {
                FirestoreRepository.initRoom(context)
                HeroRepository.initRoom(context)
            }

            try {
                // 2. Load data: Room first, then Firebase if needed
                val (categories, videos, heroes) = withContext(Dispatchers.IO) {
                    val cats   = FirestoreRepository.getCategories()
                    val vids   = FirestoreRepository.getVideos()
                    val heroes = HeroRepository.getHeroes()
                    Triple(cats, vids, heroes)
                }

                // 3. Start the realtime sync service (handles hourly refresh + cleanup)
                withContext(Dispatchers.IO) {
                    RealtimeSyncService.initialize(context)
                    RealtimeSyncService.startListeningWithKnownData(
                        publishedVideoIds = videos.map { it.id }.toSet(),
                        activeHeroIds     = heroes.map { it.id }.toSet(),
                        context           = context
                    )
                }

                // 4. Trigger the full offline video sync if:
                //    - device is online, AND
                //    - offload mode is enabled in settings
                if (isCurrentlyConnected() && SettingsViewModel.isOffloadEnabled(context)) {
                    backgroundScope.launch {
                        // Small delay so the UI can settle before heavy I/O starts
                        delay(500)
                        try {
                            VideoSyncManager.syncAllVideos(context, videos, heroes)
                        } catch (e: Exception) {
                            SyncManager.updateMessage("Download paused — will retry next launch")
                            delay(3000)
                            SyncManager.completeSync()
                        }
                    }
                }

            } catch (e: Exception) {
            }

            // Show splash for at least 1.5 s for branding
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1500) delay(1500 - elapsed)

            _uiState.value = SplashState.NavigateToHome
        }
    }*/

    private fun startSplashProcess() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                FirestoreRepository.initRoom(context)
                HeroRepository.initRoom(context)
            }

            try {
                val (categories, videos, heroes) = withContext(Dispatchers.IO) {
                    val cats   = FirestoreRepository.getCategories()
                    val vids   = FirestoreRepository.getVideos()
                    val heroes = HeroRepository.getHeroes()
                    Triple(cats, vids, heroes)
                }

                withContext(Dispatchers.IO) {
                    RealtimeSyncService.initialize(context)
                    RealtimeSyncService.startListeningWithKnownData(
                        publishedVideoIds = videos.map { it.id }.toSet(),
                        activeHeroIds     = heroes.map { it.id }.toSet(),
                        context           = context
                    )
                }

                if (isCurrentlyConnected() && SettingsViewModel.isOffloadEnabled(context)) {
                    VideoSyncManager.startSync(context, videos, heroes)  // ← one line
                }

            } catch (e: Exception) {
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1500) delay(1500 - elapsed)
            _uiState.value = SplashState.NavigateToHome
        }
    }

    fun retry() {
        _uiState.value = SplashState.Loading
        startSplashProcess()
    }

    private fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

}