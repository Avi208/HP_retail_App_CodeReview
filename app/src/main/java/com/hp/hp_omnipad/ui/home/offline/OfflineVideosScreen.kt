package com.hp.hp_omnipad.ui.home.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.utils.SafeFilePaths
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OfflineVideosScreen(
    onVideoSelected: (String) -> Unit
) {
    // Specify the list type explicitly
    var videos by remember { mutableStateOf<List<VideoSyncManager.VideoMetadata>>(emptyList()) }
    var videoToDelete by remember { mutableStateOf<VideoSyncManager.VideoMetadata?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) 4 else 2
    
    fun refreshList() {
        videos = VideoSyncManager.getAllDownloadedVideos()
    }

    // Pull to refresh state with explicit type inference
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                // Explicitly specify the return type for withContext
                val refreshedVideos = withContext<List<VideoSyncManager.VideoMetadata>>(Dispatchers.IO) {
                    VideoSyncManager.getAllDownloadedVideos()
                }
                videos = refreshedVideos
                
                // Retry missing thumbnails with explicit type
                val retriedCount: Int = withContext(Dispatchers.IO) {
                    VideoSyncManager.retryMissingThumbnails()
                }
                
                if (retriedCount > 0) {
                    val updatedVideos = withContext<List<VideoSyncManager.VideoMetadata>>(Dispatchers.IO) {
                        VideoSyncManager.getAllDownloadedVideos()
                    }
                    videos = updatedVideos
                }
                delay(300)
                isRefreshing = false
            }
        }
    )
    
    // Refresh on screen visibility
    LaunchedEffect(Unit) {
        refreshList()
        
        // Background thumbnail retry
        val retriedCount: Int = withContext(Dispatchers.IO) {
            VideoSyncManager.retryMissingThumbnails()
        }
        if (retriedCount > 0) refreshList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.no_downloaded_videos),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.videos_download_appear),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { refreshList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.videos_downloaded, videos.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { refreshList() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(videos, key = { it.id }) { video ->
                        OfflineVideoCard(
                            video = video,
                            onPlay = {
                                val path = safeLocalVideoPath(video.id)
                                if (path != null) onVideoSelected(path)
                            },
                            onDelete = { videoToDelete = video }
                        )
                    }
                }
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Delete confirmation dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_video)) },
            text = {
                Text(stringResource(R.string.delete_video_confirm, video.title, ""))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val folder = VideoSyncManager.findVideoFolder(video.id)
                        folder?.deleteRecursively()
                        refreshList()
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun OfflineVideoCard(
    video: VideoSyncManager.VideoMetadata,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val thumbnailUri = remember(video.id) { safeLocalThumbnailUri(video.id) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                if (thumbnailUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color(0xFF22C55E),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.offline).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Validated local thumbnail URI for Coil (CWE-73). */
private fun safeLocalThumbnailUri(videoId: String): Uri? {
    val safeId = SafeFilePaths.sanitizeVideoId(videoId) ?: return null
    return VideoSyncManager.getLocalThumbnailUri(safeId)
}

/** Validated local video path for playback (CWE-73). */
private fun safeLocalVideoPath(videoId: String): String? {
    val safeId = SafeFilePaths.sanitizeVideoId(videoId) ?: return null
    return VideoSyncManager.getLocalVideoPath(safeId)
}
