package com.hp.hp_omnipad.ui.home.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.utils.VideoSyncManager
import java.io.File

private const val TAG = "VideoCard"

@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit
) {

    val context = LocalContext.current
    
    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    // Check if video is downloaded DYNAMICALLY (not cached)
    // This ensures the flag updates when video is deleted
    var isDownloaded by remember { mutableStateOf(false) }
    var localThumbnailPath by remember { mutableStateOf<String?>(null) }
    
    // Check download status on every composition
    LaunchedEffect(video.id) {
        isDownloaded = VideoSyncManager.isVideoDownloaded(video.id)
        localThumbnailPath = if (isDownloaded) {
            VideoSyncManager.getLocalThumbnailPath(video.id)
        } else null
        
        // Log render source
        if (isDownloaded) {
            Log.d(TAG, " RENDERING OFFLINE: ${video.title} (ID: ${video.id})")
        } else {
            Log.d(TAG, " RENDERING ONLINE: ${video.title} (ID: ${video.id})")
        }
    }
    
    // Use local thumbnail if downloaded and file exists, otherwise use remote URL
    val thumbnailSource: Any? = if (isDownloaded && !localThumbnailPath.isNullOrEmpty() && File(localThumbnailPath).exists()) {
        File(localThumbnailPath)
    } else if (video.thumbnailUrl.isNotEmpty()) {
        video.thumbnailUrl
    } else {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer {
                shadowElevation = if (isPressed) 2.dp.toPx() else 6.dp.toPx()
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column {

            // ---------------- Thumbnail Section ----------------
            Box {

                if (thumbnailSource != null) {

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailSource)
                            .crossfade(true)
                            .memoryCacheKey("${video.id}_thumb")
                            .diskCacheKey("${video.id}_thumb")
                            .build(),
                        contentDescription = video.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Downloaded badge (shows when video is available offline)
                if (isDownloaded) {
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
                                "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ---------- Bottom gradient overlay ----------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // ---------------- Play Button ----------------
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape
                        )
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // ---------------- Duration Badge ----------------
                if (video.duration.isNotEmpty()) {

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {

                        Text(
                            text = video.duration,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // ---------------- Text Section ----------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {

                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (video.views.isNotEmpty()) {

                    Text(
                        text = video.views,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}