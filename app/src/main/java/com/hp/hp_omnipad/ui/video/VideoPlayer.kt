package com.hp.hp_omnipad.ui.video

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun VideoPlayer(
    videoUrl: String,
    isFullScreen: Boolean,
    onFullScreenToggle: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    exoPlayer: ExoPlayer,
    availableQualities: List<VideoQuality> = emptyList(),
    selectedQuality: String = "Auto",
    onQualityChange: (String) -> Unit = {},
    isOfflineMode: Boolean = videoUrl.startsWith("/"),
    availableSubtitles: List<SubtitleTrack> = emptyList(),
    captionsEnabled: Boolean = false,
    selectedSubtitle: SubtitleTrack? = null,
    onCaptionsToggle: () -> Unit = {},
    onSubtitleSelect: (SubtitleTrack) -> Unit = {},
    modifier: Modifier = Modifier,
    autoplayEnabled: Boolean = true,
    onAutoplayToggle: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Audio Manager for volume control
    val audioManager = remember { context.getSystemService(Activity.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var volumeBeforeMute by remember { mutableStateOf(currentVolume) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(currentVolume == 0) }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var showQualitySelector by remember { mutableStateOf(false) }
    var showCaptionSelector by remember { mutableStateOf(false) }


    // Sync playing state
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Keep screen awake
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Progress and volume sync updates
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (!isUserSeeking) currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            // Sync volume with system
            val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (systemVolume != currentVolume) {
                currentVolume = systemVolume
                isMuted = systemVolume == 0
            }
            delay(300)
        }
    }

    // Handle Fullscreen UI
    LaunchedEffect(isFullScreen) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                // Allow bars to show temporarily when swiping from edge
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.setDecorFitsSystemWindows(window, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullScreen) {
        onFullScreenToggle(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { onFullScreenToggle(!isFullScreen) }
                )
            }
    ) {
        // Use AndroidView without 'key(isFullScreen)'. Reusing the PlayerView is safer.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    // Using default SurfaceView is generally more stable unless you
                    // specifically need TextureView for animations/transparency
                    player = exoPlayer
                }
            },
            update = { playerView ->
                // Ensure player is attached if view is updated
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls - back button with proper insets for fullscreen
        if (onBack != null) {
            IconButton(
                onClick = { if (isFullScreen) onFullScreenToggle(false) else onBack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(if (isFullScreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        }

        // ✅ REPLACE WITH - shows ⏸️/▶️ in center whenever controls are visible
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (isFullScreen) Modifier.navigationBarsPadding() else Modifier)
        ) {
            VideoControls(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSeek = {
                    currentPosition = it
                    isUserSeeking = true
                },
                onSeekFinished = {
                    exoPlayer.seekTo(currentPosition)
                    isUserSeeking = false
                },
                onRewind = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) },
                onForward = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) },
                isFullScreen = isFullScreen,
                onToggleFullScreen = { onFullScreenToggle(!isFullScreen) },
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                isMuted = isMuted,
                showVolumeSlider = showVolumeSlider,
                onMuteToggle = {
                    if (isMuted) {
                        val restoreVolume = if (volumeBeforeMute > 0) volumeBeforeMute else maxVolume / 2
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVolume, 0)
                        currentVolume = restoreVolume
                        isMuted = false
                    } else {
                        volumeBeforeMute = currentVolume
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                        currentVolume = 0
                        isMuted = true
                    }
                },
                onVolumeSliderToggle = { showVolumeSlider = !showVolumeSlider },
                onVolumeChange = { newVolume ->
                    currentVolume = newVolume
                    isMuted = newVolume == 0
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                },
                availableQualities = availableQualities,
                selectedQuality = selectedQuality,
                showQualitySelector = showQualitySelector,
                onQualitySelectorToggle = { showQualitySelector = !showQualitySelector },
                onQualityChange = { quality ->
                    onQualityChange(quality)
                    showQualitySelector = false
                },
                isOfflineMode = isOfflineMode,
                availableSubtitles = availableSubtitles,
                captionsEnabled = captionsEnabled,
                selectedSubtitle = selectedSubtitle,
                showCaptionSelector = showCaptionSelector,
                onCaptionSelectorToggle = { showCaptionSelector = !showCaptionSelector },
                onCaptionsToggle = onCaptionsToggle,
                onSubtitleSelect = { subtitle ->
                    onSubtitleSelect(subtitle)
                    showCaptionSelector = false
                },
                autoplayEnabled = autoplayEnabled,
                onAutoplayToggle = onAutoplayToggle,

            )
        }
    }
}

@Composable
private fun VideoControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    currentVolume: Int,
    maxVolume: Int,
    isMuted: Boolean,
    showVolumeSlider: Boolean,
    onMuteToggle: () -> Unit,
    onVolumeSliderToggle: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    availableQualities: List<VideoQuality> = emptyList(),
    selectedQuality: String = "Auto",
    showQualitySelector: Boolean = false,
    onQualitySelectorToggle: () -> Unit = {},
    onQualityChange: (String) -> Unit = {},
    isOfflineMode: Boolean = false,
    availableSubtitles: List<SubtitleTrack> = emptyList(),
    captionsEnabled: Boolean = false,
    selectedSubtitle: SubtitleTrack? = null,
    showCaptionSelector: Boolean = false,
    onCaptionSelectorToggle: () -> Unit = {},
    onCaptionsToggle: () -> Unit = {},
    onSubtitleSelect: (SubtitleTrack) -> Unit = {},
    autoplayEnabled: Boolean,
    onAutoplayToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // YouTube-style progress bar
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            onValueChangeFinished = onSeekFinished,
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRewind) { Icon(Icons.Default.Replay10, null, tint = Color.White) }
                IconButton(onClick = onPlayPause) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                }
                IconButton(onClick = onForward) { Icon(Icons.Default.Forward10, null, tint = Color.White) }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Autoplay toggle FIRST
                IconButton(onClick ={
                    Log.d("AUTOPLAY_UI", "Button clicked")
                    onAutoplayToggle()
                },
                    modifier = Modifier
                        .background(
                            if (!autoplayEnabled)
                                Color.White.copy(alpha = 0.15f) // highlight when OFF (loop mode)
                            else
                                Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription =
                            if (autoplayEnabled) "Autoplay On" else "Loop",
                        tint =
                            if (autoplayEnabled)
                                Color.White.copy(alpha = 0.4f) // 🔁 dim → autoplay ON
                            else
                                Color.White                  // 🔁 solid → loop mode
                    )
                }
                // Volume Control with mute toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (showVolumeSlider) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        .padding(horizontal = if (showVolumeSlider) 8.dp else 0.dp)
                ) {
                    // Volume icon - single click mutes, long press or when slider visible toggles slider
                    IconButton(
                        onClick = {
                            if (showVolumeSlider) {
                                // If slider is visible, clicking icon closes it
                                onVolumeSliderToggle()
                            } else {
                                // If slider is hidden, clicking mutes/unmutes
                                onMuteToggle()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when {
                                isMuted || currentVolume == 0 -> Icons.Default.VolumeOff
                                currentVolume < maxVolume / 2 -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White
                        )
                    }
                    
                    AnimatedVisibility(visible = showVolumeSlider) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = currentVolume.toFloat(),
                                onValueChange = { onVolumeChange(it.toInt()) },
                                valueRange = 0f..maxVolume.toFloat(),
                                modifier = Modifier.width(100.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            // Close button for slider
                            IconButton(
                                onClick = onVolumeSliderToggle,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close volume",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // Show slider toggle when slider is hidden
                    if (!showVolumeSlider) {
                        IconButton(onClick = onVolumeSliderToggle, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Volume slider",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // Captions/CC button
                Box {
                    IconButton(onClick = {
                        if (availableSubtitles.isEmpty()) {
                            onCaptionsToggle()
                        } else {
                            onCaptionSelectorToggle()
                        }
                    }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    if (captionsEnabled) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ClosedCaption,
                                contentDescription = "Captions",
                                tint = if (captionsEnabled) Color.Cyan else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            if (captionsEnabled && selectedSubtitle != null) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = selectedSubtitle.label.take(2).uppercase(),
                                    color = Color.Cyan,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    // Subtitle language selector dropdown
                    if (availableSubtitles.isNotEmpty()) {
                        DropdownMenu(
                            expanded = showCaptionSelector,
                            onDismissRequest = onCaptionSelectorToggle,
                            modifier = Modifier.background(Color(0xFF212121))
                        ) {
                            // Off option
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Off",
                                            color = if (!captionsEnabled) Color.Cyan else Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (!captionsEnabled) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Cyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onCaptionsToggle()
                                    onCaptionSelectorToggle()
                                }
                            )
                            
                            // Available subtitle tracks
                            availableSubtitles.forEach { subtitle ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = subtitle.label,
                                                color = if (captionsEnabled && selectedSubtitle?.language == subtitle.language) Color.Cyan else Color.White,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (captionsEnabled && selectedSubtitle?.language == subtitle.language) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Cyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onSubtitleSelect(subtitle) }
                                )
                            }
                        }
                    }
                }
                
                // Quality selector button (only show when streaming online, not for offline videos)
                if (availableQualities.isNotEmpty() && !isOfflineMode) {
                    Box {
                        IconButton(onClick = onQualitySelectorToggle) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        if (showQualitySelector) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.HighQuality,
                                    contentDescription = "Quality",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = selectedQuality,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showQualitySelector,
                            onDismissRequest = onQualitySelectorToggle,
                            modifier = Modifier.background(Color(0xFF212121))
                        ) {
                            availableQualities.forEach { quality ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = quality.label,
                                                color = if (selectedQuality == quality.label) Color.Cyan else Color.White,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (quality.isAuto) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "(Recommended)",
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (selectedQuality == quality.label) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Cyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onQualityChange(quality.label) }
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onToggleFullScreen) {
                    Icon(
                        if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                        null, 
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}