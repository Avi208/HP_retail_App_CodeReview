package com.hp.hp_omnipad.ui.playlist

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.ui.theme.AppBlue
import com.hp.hp_omnipad.ui.video.VideoPlayer
import com.hp.hp_omnipad.ui.video.VideoPlayerViewModel
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.launch
import java.io.File

/*
 * Description: Premium playlist screen for playing category videos in sequence
 * Params: categoryName - name of category, videos - list of videos, startIndex - initial video
 * Returns: Full screen playlist UI with video player and video list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    categoryName: String,
    videos: List<VideoItem>,
    startIndex: Int = 0,
    onBack: () -> Unit,
    onIncrementView: (String) -> Unit = {},
    playerViewModel: VideoPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    
    var currentVideoIndex by remember { mutableStateOf(startIndex.coerceIn(0, videos.lastIndex.coerceAtLeast(0))) }
    var isFullScreen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    val availableQualities by playerViewModel.availableQualities.collectAsState()
    val selectedQuality by playerViewModel.selectedQuality.collectAsState()
    
    // Autoplay & Caption state
    val autoplayEnabled by playerViewModel.autoplayEnabled.collectAsState()
    val availableSubtitles by playerViewModel.availableSubtitles.collectAsState()
    val captionsEnabled by playerViewModel.captionsEnabled.collectAsState()
    val selectedSubtitle by playerViewModel.selectedSubtitle.collectAsState()
    
    val currentVideo = videos.getOrNull(currentVideoIndex)
    var currentVideoUrl by remember(currentVideo?.id) { mutableStateOf(currentVideo?.videoUrl ?: "") }
    
    // Update video URL when video changes
    LaunchedEffect(currentVideo?.id) {
        currentVideo?.let { video ->
            currentVideoUrl = if (VideoSyncManager.isVideoDownloaded(video.id)) {
                VideoSyncManager.getLocalVideoPath(video.id) ?: video.videoUrl
            } else {
                video.videoUrl
            }
        }
    }
    
    // Prepare video when URL changes
    LaunchedEffect(currentVideoIndex, currentVideoUrl) {
        if (currentVideoUrl.isNotEmpty()) {
            playerViewModel.prepare(currentVideoUrl, autoPlay = true)
            currentVideo?.id?.let { id ->
                onIncrementView(id)
            }
        }
    }
    
    // Initial scroll to start index
    LaunchedEffect(Unit) {
        if (startIndex > 0) {
            listState.scrollToItem(startIndex)
        }
    }
    
    // Auto-advance to next video
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                // Advance only if autoplay is ON
                if (state == Player.STATE_ENDED && playerViewModel.autoplayEnabled.value && currentVideoIndex < videos.size - 1) {
                    currentVideoIndex++
                    scope.launch {
                        listState.animateScrollToItem(currentVideoIndex)
                    }
                }
            }
        }
        playerViewModel.exoPlayer.addListener(listener)
        
        onDispose {
            playerViewModel.exoPlayer.removeListener(listener)
            playerViewModel.exoPlayer.stop()
        }
    }
    
    // Scroll to current video when index changes
    LaunchedEffect(currentVideoIndex) {
        listState.animateScrollToItem(currentVideoIndex)
    }
    
    // Handle fullscreen system UI
    LaunchedEffect(isFullScreen) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            playerViewModel.exoPlayer.stop()
            onBack()
        }
    }
    
    // Main layout - use Box to allow fullscreen overlay
    Box(modifier = Modifier.fillMaxSize()) {
        // Normal content (hidden when fullscreen)
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                if (isLandscape) {
                    // Landscape layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Video Player - Left (60%)
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight()
                                .background(Color.Black)
                        ) {
                            VideoPlayer(
                                videoUrl = currentVideoUrl,
                                exoPlayer = playerViewModel.exoPlayer,
                                isFullScreen = false,
                                onFullScreenToggle = { isFullScreen = true },
                                onBack = {
                                    playerViewModel.exoPlayer.stop()
                                    onBack()
                                },
                                availableQualities = availableQualities,
                                selectedQuality = selectedQuality,
                                onQualityChange = { playerViewModel.setVideoQuality(it) },
                                availableSubtitles = availableSubtitles,
                                captionsEnabled = captionsEnabled,
                                selectedSubtitle = selectedSubtitle,
                                onCaptionsToggle = { playerViewModel.toggleCaptions() },
                                onSubtitleSelect = { playerViewModel.selectSubtitle(it) },
                                autoplayEnabled = autoplayEnabled,
                                onAutoplayToggle = { playerViewModel.toggleAutoplay() }
                            )
                        }
                        
                        // Playlist sidebar - Right (40%)
                        Surface(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            Column {
                                // Header
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = null,
                                                tint = AppBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = { (currentVideoIndex + 1).toFloat() / videos.size },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = AppBlue,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = stringResource(R.string.playing_video_count, currentVideoIndex + 1, videos.size),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Video list
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(videos) { index, video ->
                                        PlaylistVideoCard(
                                            video = video,
                                            index = index + 1,
                                            isPlaying = index == currentVideoIndex,
                                            onClick = { currentVideoIndex = index }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Top bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    playerViewModel.exoPlayer.stop()
                                    onBack()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.video_count_label, currentVideoIndex + 1, videos.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Video Player
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            VideoPlayer(
                                videoUrl = currentVideoUrl,
                                exoPlayer = playerViewModel.exoPlayer,
                                isFullScreen = false,
                                onFullScreenToggle = { isFullScreen = true },
                                availableQualities = availableQualities,
                                selectedQuality = selectedQuality,
                                onQualityChange = { playerViewModel.setVideoQuality(it) },
                                availableSubtitles = availableSubtitles,
                                captionsEnabled = captionsEnabled,
                                selectedSubtitle = selectedSubtitle,
                                onCaptionsToggle = { playerViewModel.toggleCaptions() },
                                onSubtitleSelect = { playerViewModel.selectSubtitle(it) },
                                autoplayEnabled = autoplayEnabled,
                                onAutoplayToggle = { playerViewModel.toggleAutoplay() }
                            )
                        }
                        
                        // Video info card
                        currentVideo?.let { video ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = video.views,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = video.duration,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Playlist header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.up_next),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${videos.size - currentVideoIndex - 1} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Video list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(videos) { index, video ->
                                PlaylistVideoCard(
                                    video = video,
                                    index = index + 1,
                                    isPlaying = index == currentVideoIndex,
                                    onClick = { currentVideoIndex = index }
                                )
                            }
                            
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
        
        // Fullscreen overlay - same VideoPlayer, just fills screen
        AnimatedVisibility(
            visible = isFullScreen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayer(
                    videoUrl = currentVideoUrl,
                    exoPlayer = playerViewModel.exoPlayer,
                    isFullScreen = true,
                    onFullScreenToggle = { isFullScreen = false },
                    onBack = { isFullScreen = false },
                    availableQualities = availableQualities,
                    selectedQuality = selectedQuality,
                    onQualityChange = { playerViewModel.setVideoQuality(it) },
                    availableSubtitles = availableSubtitles,
                    captionsEnabled = captionsEnabled,
                    selectedSubtitle = selectedSubtitle,
                    onCaptionsToggle = { playerViewModel.toggleCaptions() },
                    onSubtitleSelect = { playerViewModel.selectSubtitle(it) },
                    autoplayEnabled = autoplayEnabled,
                    onAutoplayToggle = { playerViewModel.toggleAutoplay() }
                )
            }
        }
    }
}

/*
 * Description: Premium video card for playlist with large thumbnail
 * Params: video - VideoItem data, index - position number, isPlaying - current playback state
 * Returns: Card UI with thumbnail, title, and status
 */
@Composable
private fun PlaylistVideoCard(
    video: VideoItem,
    index: Int,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    var localThumbnailPath by remember(video.id) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(video.id) {
        localThumbnailPath = VideoSyncManager.getLocalThumbnailPath(video.id)
    }
    
    val thumbnailModel = if (localThumbnailPath != null && File(localThumbnailPath!!).exists()) {
        File(localThumbnailPath!!)
    } else {
        video.thumbnailUrl
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isPlaying) {
                    Modifier.border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(AppBlue, Color(0xFF00D4FF))),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) 
                AppBlue.copy(alpha = 0.08f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index/Playing indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isPlaying) AppBlue else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    NowPlayingBars()
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Large thumbnail
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(4.dp, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 50f
                            )
                        )
                )
                
                // Duration badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = video.duration,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Playing indicator overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppBlue.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AppBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Video info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isPlaying) AppBlue else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = video.views,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isPlaying) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = AppBlue,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.now_playing_section).uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

/*
 * Description: Animated bars indicating now playing status
 * Params: None
 * Returns: Animated equalizer-style bars
 */
@Composable
private fun NowPlayingBars() {
    var phase by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(200)
            phase = (phase + 1) % 4
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        repeat(3) { i ->
            val height by animateFloatAsState(
                targetValue = when ((phase + i) % 4) {
                    0 -> 0.3f
                    1 -> 0.6f
                    2 -> 1f
                    else -> 0.5f
                },
                animationSpec = tween(150),
                label = "bar$i"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(height)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }
    }
}