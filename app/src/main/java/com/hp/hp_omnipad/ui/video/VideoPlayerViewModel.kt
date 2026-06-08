package com.hp.hp_omnipad.ui.video

import android.app.Application
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.hp.hp_omnipad.ui.home.settings.SettingsViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.utils.SafeFilePaths
import com.hp.hp_omnipad.utils.SafeLog

data class VideoQuality(
    val label: String,
    val height: Int,
    val isAuto: Boolean = false
)

data class SubtitleTrack(
    val label: String,
    val language: String,
    val trackIndex: Int,
    val groupIndex: Int
)

class VideoPlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val TAG = "VideoPlayerViewModel"

    // ---------------- Track Selector for ABR ----------------
    private val trackSelector = DefaultTrackSelector(appContext).apply {
        setParameters(
            buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
        )
    }

    // ── Optimized LoadControl — faster startup without sacrificing stability ──
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,   // minBufferMs
            50_000,   // maxBufferMs
            500,      // bufferForPlaybackMs (Reduced from 2500ms default for fast start)
            1_000     // bufferForPlaybackAfterRebufferMs
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    // ---------------- ExoPlayer with ABR support ----------------
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .build()

    private var currentVideoUrl: String? = null
    private var playbackPosition: Long = 0L
    private var isPlayingLocalFile: Boolean = false

    // ---------------- Video Quality Selection ----------------
    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities = _availableQualities.asStateFlow()
    
    private val _selectedQuality = MutableStateFlow(
        SettingsViewModel.getVideoQuality(appContext)
    )
    val selectedQuality = _selectedQuality.asStateFlow()
    
    // ---------------- Captions/Subtitles ----------------
    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitles = _availableSubtitles.asStateFlow()
    
    private val _captionsEnabled = MutableStateFlow(false)
    val captionsEnabled = _captionsEnabled.asStateFlow()
    
    private val _selectedSubtitle = MutableStateFlow<SubtitleTrack?>(null)
    val selectedSubtitle = _selectedSubtitle.asStateFlow()

    // ---------------- Audio Manager for Volume Control ----------------
    private val audioManager = appContext.getSystemService(Application.AUDIO_SERVICE) as AudioManager

    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private val _currentVolume = MutableStateFlow(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    val currentVolume = _currentVolume.asStateFlow()

    // ---------------- Network ----------------
    private val connectivityManager =
        appContext.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkCurrentConnection())
    val isConnected = _isConnected.asStateFlow()

    // ---------------- Autoplay ----------------
    private val _autoplayEnabled = MutableStateFlow(
        SettingsViewModel.isAutoplayEnabled(appContext)
    )
    val autoplayEnabled = _autoplayEnabled.asStateFlow()

    init {
        observeNetworkChanges()
        setupTrackListener()
        applyInitialQualitySetting()

        // Apply autoplay mode on init
        exoPlayer.repeatMode =
            if (_autoplayEnabled.value)
                Player.REPEAT_MODE_OFF
            else
                Player.REPEAT_MODE_ONE
    }
    
    private fun setupTrackListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableQualities(tracks)
                updateAvailableSubtitles(tracks)
            }
        })
    }
    
    private fun updateAvailableSubtitles(tracks: Tracks) {
        val subtitles = mutableListOf<SubtitleTrack>()
        
        for ((groupIndex, trackGroup) in tracks.groups.withIndex()) {
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val language = format.language ?: "unknown"
                    val label = getLanguageLabel(language)
                    
                    subtitles.add(SubtitleTrack(
                        label = label,
                        language = language,
                        trackIndex = i,
                        groupIndex = groupIndex
                    ))
                    SafeLog.d(TAG, "Found subtitle track: %s (%s)", label, language)
                }
            }
        }
        
        _availableSubtitles.value = subtitles
        SafeLog.d(TAG, "Available subtitles: %s", subtitles.joinToString { it.label })
    }
    
    private fun getLanguageLabel(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "en", "eng" -> "English"
            "hi", "hin" -> "Hindi"
            "es", "spa" -> "Spanish"
            "fr", "fra" -> "French"
            "de", "deu" -> "German"
            "ja", "jpn" -> "Japanese"
            "ko", "kor" -> "Korean"
            "zh", "zho" -> "Chinese"
            "ar", "ara" -> "Arabic"
            "pt", "por" -> "Portuguese"
            "ru", "rus" -> "Russian"
            else -> languageCode.uppercase()
        }
    }
    
    fun toggleCaptions() {
        _captionsEnabled.value = !_captionsEnabled.value
        
        if (_captionsEnabled.value) {
            // Enable captions - select first available or previously selected
            val subtitleToSelect = _selectedSubtitle.value ?: _availableSubtitles.value.firstOrNull()
            subtitleToSelect?.let { selectSubtitle(it) }
        } else {
            // Disable captions
            disableSubtitles()
        }
        
        SafeLog.d(TAG, "Captions toggled: %s", _captionsEnabled.value)
    }
    
    fun selectSubtitle(subtitle: SubtitleTrack) {
        _selectedSubtitle.value = subtitle
        _captionsEnabled.value = true
        
        val tracks = exoPlayer.currentTracks
        for ((groupIndex, trackGroup) in tracks.groups.withIndex()) {
            if (trackGroup.type == C.TRACK_TYPE_TEXT && groupIndex == subtitle.groupIndex) {
                val override = TrackSelectionOverride(
                    trackGroup.mediaTrackGroup,
                    listOf(subtitle.trackIndex)
                )
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .addOverride(override)
                        .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT.inv())
                )
                SafeLog.d(TAG, "Selected subtitle: %s", subtitle.label)
                break
            }
        }
    }
    
    private fun disableSubtitles() {
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
        )
        SafeLog.d(TAG, "Subtitles disabled")
    }
    
    fun setCaptionsEnabled(enabled: Boolean) {
        if (enabled != _captionsEnabled.value) {
            toggleCaptions()
        }
    }
    
    private fun updateAvailableQualities(tracks: Tracks) {
        val qualities = mutableListOf<VideoQuality>()
        qualities.add(VideoQuality("Auto", 0, isAuto = true))
        
        // Collect all available video heights from tracks
        val availableHeights = mutableSetOf<Int>()
        var trackCount = 0
        
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    if (format.height > 0) {
                        availableHeights.add(format.height)
                        trackCount++
                    }
                }
            }
        }
        
        val maxHeight = availableHeights.maxOrNull() ?: 0
        
        // For adaptive streams (HLS/DASH) with multiple tracks, use actual available qualities
        if (trackCount > 1 && availableHeights.isNotEmpty()) {
            availableHeights.sortedDescending().forEach { height ->
                qualities.add(VideoQuality("${height}p", height))
            }
            SafeLog.d(TAG, "Adaptive stream detected with %s video tracks", trackCount)
        } else {
            // For single-track MP4 files, provide standard quality options as rendering constraints
            val standardQualities = listOf(1080, 720, 480, 360, 240)
            
            for (height in standardQualities) {
                if (maxHeight == 0 || height <= maxHeight) {
                    qualities.add(VideoQuality("${height}p", height))
                }
            }
            
            // Add actual source resolution if non-standard
            if (maxHeight > 0 && !standardQualities.contains(maxHeight)) {
                qualities.add(1, VideoQuality("${maxHeight}p (Source)", maxHeight))
            }
            SafeLog.d(TAG, "Single-track video detected, source: %sp", maxHeight)
        }
        
        // Sort: Auto first, then by height descending
        qualities.sortWith(compareBy({ !it.isAuto }, { -it.height }))
        _availableQualities.value = qualities
        SafeLog.d(TAG, "Available qualities: %s", qualities.joinToString { it.label })
    }
    
    private fun applyInitialQualitySetting() {
        val savedQuality = SettingsViewModel.getVideoQuality(appContext)
        setVideoQuality(savedQuality)
    }
    
    fun setVideoQuality(quality: String) {
        _selectedQuality.value = quality
        SafeLog.d(TAG, "Setting video quality to: %s", quality)
        
        val currentPosition = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying
        
        if (quality == "Auto") {
            // Auto mode: let ExoPlayer choose based on bandwidth
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .clearVideoSizeConstraints()
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setForceHighestSupportedBitrate(false)
            )
        } else {
            val height = quality.replace("p", "").toIntOrNull() ?: return
            
            // For manual quality selection, we need to:
            // 1. Set max height constraint
            // 2. For adaptive streams, this will select the appropriate track
            // 3. For single-track MP4, this constrains rendering resolution
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setMaxVideoSize(Int.MAX_VALUE, height)
                    .setForceHighestSupportedBitrate(true)
            )
            
            // Try to apply track override if we have video tracks
            applyQualityOverride(height)
        }
        
        // Seek back to the same position after quality change
        if (exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.seekTo(currentPosition)
            if (wasPlaying) {
                exoPlayer.play()
            }
        }
        
        SafeLog.d(TAG, "Quality changed to: %s, seeking to: %s", quality, currentPosition)
    }
    
    private fun applyQualityOverride(targetHeight: Int) {
        val tracks = exoPlayer.currentTracks
        
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                // Find the best matching track for the target height
                var bestTrackIndex = -1
                var bestHeightDiff = Int.MAX_VALUE
                
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val heightDiff = kotlin.math.abs(format.height - targetHeight)
                    if (format.height <= targetHeight && heightDiff < bestHeightDiff) {
                        bestHeightDiff = heightDiff
                        bestTrackIndex = i
                    }
                }
                
                // If we found a matching track, apply an override
                if (bestTrackIndex >= 0) {
                    val override = TrackSelectionOverride(
                        trackGroup.mediaTrackGroup,
                        listOf(bestTrackIndex)
                    )
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                            .addOverride(override)
                    )
                    SafeLog.d(TAG, "Applied track override: index=%s for %sp", bestTrackIndex, targetHeight)
                }
            }
        }
    }

    fun setVolume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clampedVolume, 0)
        _currentVolume.value = clampedVolume
    }

    fun pausePlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    fun resumePlayback() {
        if (!exoPlayer.isPlaying && exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            exoPlayer.play()
        }
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkStatus().collect { connected ->
                _isConnected.value = connected

                // Only pause if streaming from internet (not for local files)
                if (!connected && !isPlayingLocalFile) {
                    exoPlayer.pause()
                }
            }
        }
    }

    private fun networkStatus(): Flow<Boolean> = callbackFlow {

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun playVideo(video: VideoItem) {
        val url = video.videoUrl
        val uri = toPlaybackUri(url) ?: run {
            SafeLog.w(TAG, "Rejected playback URL: %s", url)
            return
        }

        currentVideoUrl = url
        isPlayingLocalFile = uri.scheme == "file"

        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // Apply autoplay repeat mode
        exoPlayer.repeatMode =
            if (_autoplayEnabled.value)
                Player.REPEAT_MODE_OFF
            else
                Player.REPEAT_MODE_ONE

        SafeLog.d(TAG, "Playing next video: %s", video.title)
    }

    private fun checkCurrentConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // ---------------- Video Preparation ----------------

    fun prepare(path: String, autoPlay: Boolean = false) {
        val uri = toPlaybackUri(path) ?: run {
            SafeLog.w(TAG, "Rejected playback path: %s", path)
            return
        }

        isPlayingLocalFile = uri.scheme == "file"
        currentVideoUrl = path

        // Guard: never attempt remote load when offline
        if (!isPlayingLocalFile && !checkCurrentConnection()) {
            SafeLog.w(TAG, "Skipping prepare — offline and no local file: %s", path)
            return
        }

        // ✅ FIXED: Set playWhenReady BEFORE prepare for faster startup. 
        // Also removed redundant seekTo(0) which resets buffer.
        if (autoPlay) {
            exoPlayer.playWhenReady = true
        }
        
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        
        // Apply autoplay repeat mode
        exoPlayer.repeatMode =
            if (_autoplayEnabled.value)
                Player.REPEAT_MODE_OFF
            else
                Player.REPEAT_MODE_ONE
    }

    fun toggleAutoplay() {
            val newValue = !_autoplayEnabled.value
            _autoplayEnabled.value = newValue

        SafeLog.d("AUTOPLAY_VM", "State changed: %s", newValue)
            SettingsViewModel.setAutoplayEnabled(appContext, newValue)

            exoPlayer.repeatMode =
                if (newValue) Player.REPEAT_MODE_OFF
                else Player.REPEAT_MODE_ONE

        SafeLog.d(TAG, "Autoplay changed: %s", newValue)
        SafeLog.d("AUTOPLAY", "Toggle clicked, autoplay = %s", newValue)
    }

    fun savePlayerState() {
        playbackPosition = exoPlayer.currentPosition
    }

    private fun allowedMediaBaseDirs(): List<File> = buildList {
        add(appContext.filesDir)
        add(appContext.cacheDir)
        appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { moviesDir ->
            add(moviesDir)
            add(File(moviesDir, "OmniPad"))
        }
    }

    /**
     * Builds a safe ExoPlayer URI from a local absolute path or remote http(s) URL (CWE-73).
     */
    private fun toPlaybackUri(path: String): Uri? {
        if (path.startsWith("/")) {
            return SafeFilePaths.resolveLocalPlaybackFile(path, allowedMediaBaseDirs())?.toUri()
        }
        return try {
            val uri = Uri.parse(path)
            when (uri.scheme?.lowercase()) {
                "http", "https" -> uri
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}
