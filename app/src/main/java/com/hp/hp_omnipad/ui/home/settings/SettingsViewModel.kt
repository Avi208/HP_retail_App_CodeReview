package com.hp.hp_omnipad.ui.home.settings

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hp.hp_omnipad.data.repository.FirestoreRepository
import com.hp.hp_omnipad.data.repository.HeroRepository
import com.hp.hp_omnipad.utils.FileDownloader
import com.hp.hp_omnipad.utils.OfflineDataManager
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)

    private val _uiState = mutableStateOf(
        SettingsState(
            selectedLanguage    = getSavedLanguageName(),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            stayAwakeEnabled    = prefs.getBoolean("stay_awake_enabled", false),
            offloadAppEnabled   = prefs.getBoolean("offload_app_enabled", true),
            selectedVideoQuality = prefs.getString("video_quality", "Auto") ?: "Auto"
        )
    )
    val uiState: State<SettingsState> = _uiState

    private val _stayAwakeEnabled  = MutableStateFlow(prefs.getBoolean("stay_awake_enabled", false))
    val stayAwakeEnabled: StateFlow<Boolean> = _stayAwakeEnabled

    private val _offloadAppEnabled = MutableStateFlow(prefs.getBoolean("offload_app_enabled", true))
    val offloadAppEnabled: StateFlow<Boolean> = _offloadAppEnabled

    private val _languageChanged = MutableStateFlow(false)
    val languageChanged: StateFlow<Boolean> = _languageChanged

    fun toggleNotifications() {
        val newValue = !_uiState.value.notificationsEnabled
        _uiState.value = _uiState.value.copy(notificationsEnabled = newValue)
        prefs.edit().putBoolean("notifications_enabled", newValue).apply()
    }

    fun changeLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        val languageCode = when (language) {
            "Hindi"   -> "hi"
            "Spanish" -> "es"
            else      -> "en"
        }
        prefs.edit().putString("language_code", languageCode).apply()
        _languageChanged.value = true
    }

    fun resetLanguageChangedFlag() {
        _languageChanged.value = false
    }

    private fun getSavedLanguageName(): String {
        val code = prefs.getString("language_code", "en") ?: "en"
        return when (code) {
            "hi" -> "Hindi"
            "es" -> "Spanish"
            else -> "English"
        }
    }

    fun getSavedLanguageCode(): String =
        prefs.getString("language_code", "en") ?: "en"

    fun toggleStayAwake() {
        val newValue = !_uiState.value.stayAwakeEnabled
        _uiState.value = _uiState.value.copy(stayAwakeEnabled = newValue)
        _stayAwakeEnabled.value = newValue
        prefs.edit().putBoolean("stay_awake_enabled", newValue).apply()
    }

    fun refresh() {
        _uiState.value = SettingsState(
            selectedLanguage    = getSavedLanguageName(),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            stayAwakeEnabled    = prefs.getBoolean("stay_awake_enabled", false),
            offloadAppEnabled   = prefs.getBoolean("offload_app_enabled", true),
            selectedVideoQuality = prefs.getString("video_quality", "Auto") ?: "Auto"
        )
        _stayAwakeEnabled.value  = _uiState.value.stayAwakeEnabled
        _offloadAppEnabled.value = _uiState.value.offloadAppEnabled
    }

    // Background scope for video downloads — survives navigation
    //private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /*fun enableOffloadApp() {
        _uiState.value = _uiState.value.copy(offloadAppEnabled = true)
        _offloadAppEnabled.value = true
        prefs.edit().putBoolean("offload_app_enabled", true).apply()
        backgroundScope.launch {
            try {
                val context = getApplication<Application>().applicationContext

                // FIX: Use getVideos() / getHeroes() (cache-first) instead of
                // fetchVideosFromRemote() which always hit Firebase.
                // If Room/memory cache is fresh, zero Firebase reads needed here.
                val videos = FirestoreRepository.getVideos().also {
                    OfflineDataManager.saveVideosToCache(context, it)
                }

                val heroes = HeroRepository.getHeroes().also {
                    OfflineDataManager.saveHeroesToCache(context, it)
                }

                VideoSyncManager.syncAllVideos(context, videos, heroes)

            } catch (e: Exception) {
            }
        }
    }*/

    fun enableOffloadApp() {
        _uiState.value = _uiState.value.copy(offloadAppEnabled = true)
        _offloadAppEnabled.value = true
        prefs.edit().putBoolean("offload_app_enabled", true).apply()

        val context = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val videos = FirestoreRepository.getVideos().also {
                OfflineDataManager.saveVideosToCache(context, it)
            }
            val heroes = HeroRepository.getHeroes().also {
                OfflineDataManager.saveHeroesToCache(context, it)
            }
            VideoSyncManager.startSync(context, videos, heroes)
        }
    }

    fun disableOffloadAndDeleteVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseFolder = FileDownloader.getDownloadFolder()
                if (baseFolder.exists()) {
                    baseFolder.listFiles()?.forEach { folder ->
                        if (folder.isDirectory) {
                            folder.deleteRecursively()
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }

        _uiState.value = _uiState.value.copy(offloadAppEnabled = false)
        _offloadAppEnabled.value = false
        prefs.edit().putBoolean("offload_app_enabled", false).apply()
    }

    fun setVideoQuality(quality: String) {
        _uiState.value = _uiState.value.copy(selectedVideoQuality = quality)
        prefs.edit().putString("video_quality", quality).apply()
    }

    companion object {
        fun updateLocale(context: Context, languageCode: String): Context {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }

        fun getLanguageCode(context: Context): String {
            val prefs = context.getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)
            return prefs.getString("language_code", "en") ?: "en"
        }

        fun isOffloadEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("offload_app_enabled", true)
        }

        fun getVideoQuality(context: Context): String {
            val prefs = context.getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)
            return prefs.getString("video_quality", "Auto") ?: "Auto"
        }

        fun isAutoplayEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("autoplay_enabled", true)
        }

        fun setAutoplayEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences("hp_omnipad_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("autoplay_enabled", enabled).apply()
        }
    }
}