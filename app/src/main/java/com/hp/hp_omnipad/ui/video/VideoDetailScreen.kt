package com.hp.hp_omnipad.ui.video

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.hp.hp_omnipad.ui.home.home.HomeViewModel
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.ui.home.model.Resource
import com.hp.hp_omnipad.utils.DownloadStatus
import com.hp.hp_omnipad.utils.FileDownloader
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VideoDetailScreen(
    videoId: String?,
    localVideoPath: String? = null,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    playerViewModel: VideoPlayerViewModel = viewModel()
) {
    //val playerViewModel: VideoPlayerViewModel = viewModel(LocalContext.current as ComponentActivity)
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    //val autoplayEnabled by playerViewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val autoplayEnabled by playerViewModel.autoplayEnabled.collectAsState()

    val initialVideo: VideoItem? =
        if (localVideoPath == null)
            videoId?.let { viewModel.getVideoById(it) }
        else null

    // State for currently playing video in the detail screen
    var currentVideo by remember { mutableStateOf(initialVideo) }
    
    // Check if video is downloaded DYNAMICALLY
    var isDownloaded by remember { mutableStateOf(false) }
    var localPath by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentVideo?.id, localVideoPath) {
        isDownloaded = currentVideo?.id?.let { VideoSyncManager.isVideoDownloaded(it) } ?: false
        localPath = if (isDownloaded) currentVideo?.id?.let { VideoSyncManager.getLocalVideoPath(it) } else null
    }
    
    val videoUrl = localVideoPath ?: localPath ?: currentVideo?.videoUrl ?: ""
    val isPlayingOffline = localVideoPath != null || localPath != null

    val availableQualities by playerViewModel.availableQualities.collectAsStateWithLifecycle()
    val selectedQuality by playerViewModel.selectedQuality.collectAsStateWithLifecycle()
    val availableSubtitles by playerViewModel.availableSubtitles.collectAsStateWithLifecycle()
    val captionsEnabled by playerViewModel.captionsEnabled.collectAsStateWithLifecycle()
    val selectedSubtitle by playerViewModel.selectedSubtitle.collectAsStateWithLifecycle()

    var isFullScreen by remember { mutableStateOf(false) }

    // 🔥 Prevent multiple autoplay triggers
    var handledEnd by remember { mutableStateOf(false) }

    LaunchedEffect(videoUrl) {
        handledEnd = false
        if (videoUrl.isNotEmpty()) {
            playerViewModel.prepare(videoUrl, autoPlay = true)
            currentVideo?.id?.let { id ->
                viewModel.incrementView(id)
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.pausePlayback()
        }
    }

    DisposableEffect(playerViewModel.exoPlayer, currentVideo, autoplayEnabled) {

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                //if (state == Player.STATE_ENDED && autoplayEnabled) {
                if (state == Player.STATE_ENDED && autoplayEnabled && !handledEnd && currentVideo != null) {

                        handledEnd = true

                    val currentList = uiState.allVideos

                    if (currentVideo == null || currentList.isEmpty()) return

                    val currentIndex = currentList.indexOfFirst { it.id == currentVideo?.id }

                    val nextVideo = when {
                        currentIndex == -1 -> currentList.firstOrNull()
                        currentIndex < currentList.size - 1 -> currentList[currentIndex + 1]
                        else -> null
                    }

                    nextVideo?.let {
                        //playerViewModel.playVideo(it) // 🔥 better than direct state mutation
                        currentVideo = it
                    }
                }
            }
        }

        playerViewModel.exoPlayer.addListener(listener)

        onDispose {
            playerViewModel.exoPlayer.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back Button and "NOW PLAYING"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Animated Equalizer
                EqualizerAnimation()

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = currentVideo?.title ?: "Video Detail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // Main Content Row (75/25 split)
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                
                // Left Column: Player and Description (75%)
                Column(modifier = Modifier.weight(0.75f).fillMaxHeight()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        VideoPlayer(
                            videoUrl = videoUrl,
                            isFullScreen = false,
                            onFullScreenToggle = { isFullScreen = true },
                            onBack = onBack,
                            exoPlayer = playerViewModel.exoPlayer,
                            availableQualities = availableQualities,
                            selectedQuality = selectedQuality,
                            onQualityChange = { playerViewModel.setVideoQuality(it) },
                            availableSubtitles = availableSubtitles,
                            captionsEnabled = captionsEnabled,
                            selectedSubtitle = selectedSubtitle,
                            onCaptionsToggle = { playerViewModel.toggleCaptions() },
                            onSubtitleSelect = { playerViewModel.selectSubtitle(it) },
                            modifier = Modifier.fillMaxSize(),
                            autoplayEnabled = autoplayEnabled,
                            onAutoplayToggle = { playerViewModel.toggleAutoplay() },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = currentVideo?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = currentVideo?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Right Column: Up Next Thumbnails (25%)
                Column(modifier = Modifier.weight(0.25f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.width(3.dp).height(16.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UP NEXT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // List of videos excluding the current one
                    val upNextVideos = uiState.allVideos.filter { it.id != currentVideo?.id }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        upNextVideos.forEach { video ->
                            VideoDetailThumbnail(
                                video = video,
                                onClick = {
                                    currentVideo = video
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                VideoPlayer(
                    videoUrl = videoUrl,
                    isFullScreen = true,
                    onFullScreenToggle = { isFullScreen = false },
                    onBack = { isFullScreen = false },
                    exoPlayer = playerViewModel.exoPlayer,
                    availableQualities = availableQualities,
                    selectedQuality = selectedQuality,
                    onQualityChange = { playerViewModel.setVideoQuality(it) },
                    availableSubtitles = availableSubtitles,
                    captionsEnabled = captionsEnabled,
                    selectedSubtitle = selectedSubtitle,
                    onCaptionsToggle = { playerViewModel.toggleCaptions() },
                    onSubtitleSelect = { playerViewModel.selectSubtitle(it) },
                    modifier = Modifier.fillMaxSize(),
                    autoplayEnabled = autoplayEnabled,
                    onAutoplayToggle = { playerViewModel.toggleAutoplay() },
                )
            }
        }
    }
}

@Composable
fun EqualizerAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "bar3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        Box(modifier = Modifier.width(4.dp).height(bar1Height.dp).background(Color(0xFF22C55E), RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(4.dp).height(bar2Height.dp).background(Color(0xFF22C55E), RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(4.dp).height(bar3Height.dp).background(Color(0xFF22C55E), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun VideoDetailThumbnail(
    video: VideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                )
            )

            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Center).size(32.dp)
            )

            Text(
                text = video.title,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}
