package com.hp.hp_omnipad.ui.home.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.components.CategoryCard
import com.hp.hp_omnipad.ui.home.components.VideoCard
import com.hp.hp_omnipad.ui.home.hero.HeroVideoSection
import com.hp.hp_omnipad.ui.video.VideoPlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeTabScreen(
    viewModel: HomeViewModel,
    onVideoClick: (String) -> Unit,
    onNavigateToLibrary: (String) -> Unit,
    shouldPauseHeroVideo: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playerViewModel: VideoPlayerViewModel = viewModel()
    val player = playerViewModel.exoPlayer
    val gridState = rememberLazyGridState()

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) 4 else 2
    val horizontalPadding = if (isLandscape) 28.dp else 16.dp

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // Track if the user manually clicked pause
    var userPaused by remember { mutableStateOf(false) }
    // Track previous visibility to detect the "Entering View" moment
    var wasHeroVisible by remember { mutableStateOf(true) }

    /*
     1. FIX: Background playing issue
     This observer pauses the video when the user switches tabs or leaves the app
     */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (player.isPlaying) {
                    player.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /*
     2. Detect if hero section is visible and handle Auto Play/Pause logic
     */
    LaunchedEffect(gridState.layoutInfo.visibleItemsInfo) {
        val heroVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == 0 }

        if (!heroVisible) {
            // SCROLLING AWAY: Pause the video to save resources
            if (player.isPlaying) {
                player.pause()
            }
        } else {
            // SCROLLING BACK: Only play if it was previously off-screen AND user didn't manually pause
            if (!wasHeroVisible && !userPaused && !player.isPlaying) {
                player.play()
            }
        }
        wasHeroVisible = heroVisible
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp) // Professional spacing at bottom
        ) {
            /*
            HERO VIDEO SECTION
             */
            item(span = { GridItemSpan(gridColumns) }) {
                HeroVideoSection(
                    onManualPauseToggle = { paused ->
                        userPaused = paused
                    },
                    shouldPause = shouldPauseHeroVideo
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            /*
            CATEGORIES TITLE
             */
            item(span = { GridItemSpan(gridColumns) }) {
                Text(
                    text = stringResource(R.string.categories),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            /*
            CATEGORY ROW
             */
            item(span = { GridItemSpan(gridColumns) }) {
                LazyRow(
                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = {
                                // Pause hero video before navigating
                                player.pause()
                                onNavigateToLibrary(category.slug)
                            }
                        )
                    }
                }
            }

            /*
            RECENTLY ADDED HEADER
             */
            item(span = { GridItemSpan(gridColumns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recently_added),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = { onNavigateToLibrary("all") }) {
                        Text(
                            text = stringResource(R.string.view_all),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            /*
            VIDEO GRID - Always show recently added
            VideoCard handles rendering from local storage if downloaded, otherwise from internet
             */
            items(
                items = uiState.recentlyAdded,
                key = { it.id }
            ) { video ->
                VideoCard(
                    video = video,
                    onClick = {
                        // Pause hero video before navigating
                        player.pause()
                        onVideoClick(video.id)
                    }
                )
            }

            item(span = { GridItemSpan(gridColumns) }) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}