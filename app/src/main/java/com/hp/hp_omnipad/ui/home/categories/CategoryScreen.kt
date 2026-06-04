package com.hp.hp_omnipad.ui.home.categories

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.home.HomeViewModel
import com.hp.hp_omnipad.ui.home.components.VideoCard
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.ui.theme.AppBlue

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryScreen(
    viewModel: HomeViewModel,
    selectedTab: String,
    onVideoClick: (String) -> Unit,
    onPlayAll: (String, List<VideoItem>, Int) -> Unit = { _, _, _ -> },
    onTabSelected: (String) -> Unit = {}
){

    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val gridColumns = if (isLandscape) 4 else 3

    // Pull to refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // Build category list using SLUG
    val categories = listOf("all") + uiState.categories.map { it.slug }

    // Current tab state synced with navigation argument
    var currentTab by remember(selectedTab) {
        mutableStateOf(selectedTab.lowercase())
    }

    // Ensure tab updates if navigation argument changes
    LaunchedEffect(selectedTab) {
        currentTab = selectedTab.lowercase()
    }

    // Use case-insensitive matching for index
    val selectedIndex =
        categories.indexOfFirst { it.equals(currentTab, ignoreCase = true) }.takeIf { it >= 0 } ?: 0

    // Proper filtering using categoryIds (use case-insensitive matching)
    val videos = if (currentTab.equals("all", ignoreCase = true)) {
        uiState.allVideos
    } else {
        uiState.allVideos.filter { video ->
            video.categoryIds.any { it.equals(currentTab, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AppBlue,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedIndex]),
                        height = 3.dp,
                        color = AppBlue
                    )
                }
            ) {

                categories.forEachIndexed { index, slug ->
                    val tabTitle = when (slug.lowercase()) {
                        "all" -> stringResource(R.string.view_all)
                        "demos" -> stringResource(R.string.demos)
                        "events" -> stringResource(R.string.events)
                        "promotional" -> stringResource(R.string.promotional)
                        "training" -> stringResource(R.string.training)
                        else -> slug.replaceFirstChar { it.uppercase() }
                    }

                    Tab(
                        selected = selectedIndex == index,
                        onClick = {
                            currentTab = slug
                            onTabSelected(slug)
                        },
                        text = {
                            Text(
                                text = tabTitle,
                                style = if (selectedIndex == index)
                                    MaterialTheme.typography.labelLarge
                                else
                                    MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
            
            // Play All button - show for all categories if not empty
            if (videos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.videos_count_label, videos.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            val categoryName = when (currentTab.lowercase()) {
                                "all" -> context.getString(R.string.view_all)
                                "demos" -> context.getString(R.string.demos)
                                "events" -> context.getString(R.string.events)
                                "promotional" -> context.getString(R.string.promotional)
                                "training" -> context.getString(R.string.training)
                                else -> currentTab.replaceFirstChar { it.uppercase() }
                            }
                            onPlayAll(categoryName, videos, 0)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.play_all),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                itemsIndexed(videos) { index, video ->
                    VideoCard(
                        video = video,
                        onClick = {
                            // Use playlist UI for all library videos to support auto-play
                            val categoryName = when (currentTab.lowercase()) {
                                "all" -> context.getString(R.string.view_all)
                                "demos" -> context.getString(R.string.demos)
                                "events" -> context.getString(R.string.events)
                                "promotional" -> context.getString(R.string.promotional)
                                "training" -> context.getString(R.string.training)
                                else -> currentTab.replaceFirstChar { it.uppercase() }
                            }
                            onPlayAll(categoryName, videos, index)
                        }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}