package com.hp.hp_omnipad.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FullscreenVideoScreen(
    navController: NavController,
    playerViewModel: VideoPlayerViewModel = viewModel()
) {

    val player = playerViewModel.exoPlayer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        VideoPlayer(
            videoUrl = "",
            isFullScreen = true,
            onFullScreenToggle = {
                navController.popBackStack()
            },
            onBack = {
                navController.popBackStack()
            },
            exoPlayer = player,
            modifier = Modifier.fillMaxSize()
        )
    }
}