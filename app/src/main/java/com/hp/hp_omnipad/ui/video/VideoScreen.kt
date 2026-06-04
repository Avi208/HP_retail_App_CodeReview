package com.hp.hp_omnipad.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VideoScreen(
    videoUrl: String,
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = viewModel()
) {

    var isFullScreen by remember { mutableStateOf(false) }

    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    // Prepare video once
    LaunchedEffect(videoUrl) {
        viewModel.prepare(videoUrl)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (isFullScreen) {

            VideoPlayer(
                videoUrl = videoUrl,
                isFullScreen = true,
                onFullScreenToggle = { isFullScreen = it },
                onBack = { isFullScreen = false },
                exoPlayer = viewModel.exoPlayer
            )

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                VideoPlayer(
                    videoUrl = videoUrl,
                    isFullScreen = false,
                    onFullScreenToggle = { isFullScreen = it },
                    onBack = onBack,
                    exoPlayer = viewModel.exoPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text("Video Title", style = MaterialTheme.typography.titleLarge)
                Text("Video Description goes here...")
            }
        }

        // ---------------- INTERNET LOST OVERLAY ----------------

        if (!isConnected) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(70.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Connection Lost",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Reconnecting...",
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}