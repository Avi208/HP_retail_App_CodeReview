package com.hp.hp_omnipad.ui.home.hero

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.model.Hero
import com.hp.hp_omnipad.ui.video.VideoPlayer
import com.hp.hp_omnipad.ui.video.VideoPlayerViewModel
import com.hp.hp_omnipad.utils.VideoSyncManager

private fun checkNetworkAvailability(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun HeroVideoSection(
    heroViewModel: HeroViewModel = viewModel(),
    playerViewModel: VideoPlayerViewModel = viewModel(),
    onManualPauseToggle: (Boolean) -> Unit = {},
    shouldPause: Boolean = false
) {
    val heroes by heroViewModel.heroes.collectAsState()
    val player = playerViewModel.exoPlayer
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    // Network connectivity state
    var isNetworkAvailable by remember { mutableStateOf(checkNetworkAvailability(context)) }
    
    LaunchedEffect(Unit) {
        isNetworkAvailable = checkNetworkAvailability(context)
    }
    
    val availableQualities by playerViewModel.availableQualities.collectAsState()
    val selectedQuality by playerViewModel.selectedQuality.collectAsState()
    val availableSubtitles by playerViewModel.availableSubtitles.collectAsState()
    val captionsEnabled by playerViewModel.captionsEnabled.collectAsState()
    val selectedSubtitle by playerViewModel.selectedSubtitle.collectAsState()
    val autoplayEnabled by playerViewModel.autoplayEnabled.collectAsState()

    var restoreFullscreenForYoutube by remember { mutableStateOf(false) }

    var isFullScreen by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    var wasPlayingBeforePause by remember { mutableStateOf(false) }
    
    val isViewingYoutubeState = remember { mutableStateOf(false) }
    
    SideEffect {
        isViewingYoutubeState.value = heroes.getOrNull(currentIndex)?.isYoutubeLink == true
    }
    
    val nonYoutubeHeroes = remember(heroes) {
        heroes.filter { !it.isYoutubeLink }
    }
    
    val heroIndexToExoPlayerIndex = remember(heroes) {
        var exoIndex = 0
        heroes.mapIndexed { index, hero ->
            if (hero.isYoutubeLink) {
                index to -1
            } else {
                val mapping = index to exoIndex
                exoIndex++
                mapping
            }
        }.toMap()
    }
    
    androidx.compose.runtime.SideEffect {
        if (shouldPause && player.isPlaying) {
            player.pause()
        }
    }
    
    LaunchedEffect(shouldPause) {
        if (shouldPause && player.isPlaying) {
            wasPlayingBeforePause = true
            player.pause()
        }
    }

    LaunchedEffect(Unit) {
        heroViewModel.loadHeroes()
    }

    LaunchedEffect(heroes, nonYoutubeHeroes, shouldPause) {
        if (heroes.isNotEmpty() && nonYoutubeHeroes.isNotEmpty() && player.mediaItemCount == 0) {
            val mediaItems = nonYoutubeHeroes.map { hero ->
                val isDownloaded = VideoSyncManager.isVideoDownloaded(hero.id)
                if (isDownloaded) {
                    val localPath = VideoSyncManager.getLocalVideoPath(hero.id)!!
                    MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(localPath)))
                } else {
                    MediaItem.fromUri(hero.videoUrl)
                }
            }
            player.setMediaItems(mediaItems)
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.prepare()
            
            val currentHeroIsYoutube = heroes.getOrNull(currentIndex)?.isYoutubeLink == true
            player.playWhenReady = !shouldPause && !currentHeroIsYoutube
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    player.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exoPlayerIndexToHeroIndex = remember(heroes) {
        var exoIndex = 0
        heroes.mapIndexedNotNull { heroIndex, hero ->
            if (!hero.isYoutubeLink) {
                val mapping = exoIndex to heroIndex
                exoIndex++
                mapping
            } else null
        }.toMap()
    }
    
    DisposableEffect(player, exoPlayerIndexToHeroIndex, heroIndexToExoPlayerIndex, heroes) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!isViewingYoutubeState.value) {
                    val exoPlayerIndex = player.currentMediaItemIndex
                    val heroIndex = exoPlayerIndexToHeroIndex[exoPlayerIndex] ?: exoPlayerIndex
                    currentIndex = heroIndex
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onManualPauseToggle(!isPlaying)
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !isViewingYoutubeState.value) {
                    if (playerViewModel.autoplayEnabled.value) {
                        val nextHeroIndex = (currentIndex + 1) % heroes.size
                        val nextHero = heroes.getOrNull(nextHeroIndex)
                        
                        isViewingYoutubeState.value = nextHero?.isYoutubeLink == true
                        currentIndex = nextHeroIndex
                        
                        if (nextHero?.isYoutubeLink == true) {
                            restoreFullscreenForYoutube = isFullScreen  // ← capture before Dialog closes
                            isFullScreen = false
                            player.pause()
                        } else {
                            val nextExoIndex = heroIndexToExoPlayerIndex[nextHeroIndex] ?: 0
                            if (nextExoIndex >= 0 && nextExoIndex < player.mediaItemCount) {
                                player.seekTo(nextExoIndex, 0)
                                player.playWhenReady = true
                            }
                        }
                    } else {
                        player.seekTo(0)
                        player.playWhenReady = true
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    if (heroes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
        return
    }

    val currentHero = heroes.getOrNull(currentIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Observe actual play state for equalizer
            val isPlayerPlaying by produceState(initialValue = player.isPlaying, player) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) { value = playing }
                }
                player.addListener(listener)
                awaitDispose { player.removeListener(listener) }
            }

            val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
            
            val bar1Height by infiniteTransition.animateFloat(
                initialValue = 6f,
                targetValue = if (isPlayerPlaying) 16f else 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar1"
            )
            val bar2Height by infiniteTransition.animateFloat(
                initialValue = 12f,
                targetValue = if (isPlayerPlaying) 8f else 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar2"
            )
            val bar3Height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = if (isPlayerPlaying) 14f else 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(350, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar3"
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(18.dp)
            ) {
                val equalizerColor = if (isPlayerPlaying) Color(0xFF22C55E) else Color(0xFF22C55E).copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar1Height.dp)
                        .background(equalizerColor, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar2Height.dp)
                        .background(equalizerColor, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar3Height.dp)
                        .background(equalizerColor, RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF22C55E).copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.now_playing).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22C55E),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / ${heroes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = currentHero?.title ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.75f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.25f)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.up_next).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.weight(0.75f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Box {
                    if (!isFullScreen) {
                        val isCurrentYoutube = currentHero?.isYoutubeLink == true && 
                            currentHero.youtubelink.isNotEmpty()
                        
                        when {
                            isCurrentYoutube && !isNetworkAvailable -> {
                                YouTubeOfflinePlaceholder(
                                    hero = currentHero!!,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
                            }
                            isCurrentYoutube -> {
                                LaunchedEffect(currentIndex) {
                                    if (player.isPlaying) player.pause()
                                }

                                YouTubePlayerWebView(
                                    youtubeUrl = currentHero!!.youtubelink,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                    autoplay = !shouldPause,
                                    restoreFullscreen = restoreFullscreenForYoutube,
                                    onVideoEnd = {
                                        restoreFullscreenForYoutube = false
                                        if (playerViewModel.autoplayEnabled.value) {
                                            val next = (currentIndex + 1) % heroes.size
                                            isViewingYoutubeState.value = heroes.getOrNull(next)?.isYoutubeLink == true
                                            currentIndex = next
                                            if (heroes.getOrNull(next)?.isYoutubeLink == false) {
                                                val exoIdx = heroIndexToExoPlayerIndex[next] ?: 0
                                                if (exoIdx >= 0 && exoIdx < player.mediaItemCount) {
                                                    player.seekTo(exoIdx, 0)
                                                    player.playWhenReady = true
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            else -> {
                                VideoPlayer(
                                    videoUrl = currentHero?.videoUrl ?: "",
                                    isFullScreen = false,
                                    onFullScreenToggle = { isFullScreen = it },
                                    exoPlayer = player,
                                    availableQualities = availableQualities,
                                    selectedQuality = selectedQuality,
                                    onQualityChange = { playerViewModel.setVideoQuality(it) },
                                    availableSubtitles = availableSubtitles,
                                    captionsEnabled = captionsEnabled,
                                    selectedSubtitle = selectedSubtitle,
                                    onCaptionsToggle = { playerViewModel.toggleCaptions() },
                                    onSubtitleSelect = { playerViewModel.selectSubtitle(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f),
                                    autoplayEnabled = autoplayEnabled,
                                    onAutoplayToggle = { playerViewModel.toggleAutoplay() }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            BoxWithConstraints(
                modifier = Modifier.weight(0.25f)
            ) {
                val thumbnailWidth = maxWidth
                val thumbnailHeight = thumbnailWidth * 9f / 16f
                val paddingHeight = 12.dp
                val visibleThumbnails = 2.8f
                val scrollHeight = (thumbnailHeight + paddingHeight) * visibleThumbnails

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrollHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    heroes.forEachIndexed { index, hero ->
                        HeroThumbnail(
                            hero = hero,
                            isPlaying = index == currentIndex,
                            isYoutubeVideo = hero.isYoutubeLink,
                            onClick = {
                                val nextHero = hero
                                isViewingYoutubeState.value = nextHero.isYoutubeLink
                                
                                if (nextHero.isYoutubeLink) {
                                    isNetworkAvailable = checkNetworkAvailability(context)
                                }
                                
                                currentIndex = index
                                
                                if (nextHero.isYoutubeLink) {
                                    player.pause()
                                    onManualPauseToggle(false)
                                } else {
                                    val exoPlayerIndex = heroIndexToExoPlayerIndex[index] ?: 0
                                    if (exoPlayerIndex >= 0 && exoPlayerIndex < player.mediaItemCount) {
                                        player.seekTo(exoPlayerIndex, 0)
                                        player.playWhenReady = true
                                        onManualPauseToggle(false)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val isCurrentYoutube = currentHero?.isYoutubeLink == true
    if (isFullScreen && !isCurrentYoutube) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayer(
                    videoUrl = currentHero?.videoUrl ?: "",
                    isFullScreen = true,
                    onFullScreenToggle = { isFullScreen = it },
                    onBack = { isFullScreen = false },
                    exoPlayer = player,
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
                    onAutoplayToggle = { playerViewModel.toggleAutoplay() }
                )
            }
        }
    }
}

@Composable
private fun YouTubeOfflinePlaceholder(
    hero: Hero,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = hero.thumbnailUrl,
            contentDescription = hero.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.no_internet),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.check_internet),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HeroThumbnail(
    hero: Hero,
    isPlaying: Boolean,
    isYoutubeVideo: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    var isDownloaded by remember { mutableStateOf(false) }
    var localThumbnailPath by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(hero.id) {
        isDownloaded = VideoSyncManager.isVideoDownloaded(hero.id)
        localThumbnailPath = if (isDownloaded) VideoSyncManager.getLocalThumbnailPath(hero.id) else null
    }
    
    val thumbnailSource: Any = if (isDownloaded && !localThumbnailPath.isNullOrEmpty() && java.io.File(localThumbnailPath).exists()) {
        java.io.File(localThumbnailPath)
    } else {
        hero.thumbnailUrl
    }

    val infiniteTransition = rememberInfiniteTransition(label = "borderGlow")
    
    // 🔥 OPTIMIZATION: Only animate if this thumbnail is the one currently playing
    val glowAlphaState = if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val glowAlpha by glowAlphaState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .then(
                if (isPlaying) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = primaryColor.copy(alpha = glowAlpha * 0.4f),
                        spotColor = primaryColor.copy(alpha = glowAlpha * 0.6f)
                    )
                } else {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
            .border(
                width = if (isPlaying) 2.dp else 0.dp,
                color = if (isPlaying) primaryColor.copy(alpha = glowAlpha) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            AsyncImage(
                model = thumbnailSource,
                contentDescription = hero.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            if (isYoutubeVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            Color(0xFFFF0000),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "YouTube",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            primaryColor,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.now_playing).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (!isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }

            Text(
                text = hero.title,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}
