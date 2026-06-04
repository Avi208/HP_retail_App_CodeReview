package com.hp.hp_omnipad.ui.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.navigation.BottomNavItem
import com.hp.hp_omnipad.ui.home.home.HomeTabScreen
import com.hp.hp_omnipad.ui.home.categories.LibraryScreen
import com.hp.hp_omnipad.ui.home.settings.SettingsScreen
import com.hp.hp_omnipad.ui.home.settings.SettingsViewModel
import com.hp.hp_omnipad.ui.home.settings.screens.PrivacyPolicyScreen
import com.hp.hp_omnipad.ui.home.components.TopBar
import com.hp.hp_omnipad.ui.theme.AppBlue
import com.hp.hp_omnipad.ui.home.home.HomeViewModel
import com.hp.hp_omnipad.ui.home.model.VideoItem
import com.hp.hp_omnipad.ui.playlist.PlaylistScreen
import com.hp.hp_omnipad.ui.theme.ThemeViewModel
import com.hp.hp_omnipad.ui.video.VideoDetailScreen
import com.hp.hp_omnipad.utils.RealtimeSyncService
import com.hp.hp_omnipad.utils.SyncManager
import com.hp.hp_omnipad.utils.VideoSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    themeViewModel: ThemeViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val stayAwake by settingsViewModel.stayAwakeEnabled.collectAsStateWithLifecycle()
    val syncState by SyncManager.syncState.collectAsStateWithLifecycle()

    var actualDownloadedCount by remember { mutableStateOf(0) }

    LaunchedEffect(syncState.isSyncing) {
        if (syncState.isSyncing) {
            while (true) {
                actualDownloadedCount = withContext(Dispatchers.IO) {
                    VideoSyncManager.getAllDownloadedVideos().size
                }
                delay(2_000)
            }
        }
    }
    
    var showSyncComplete by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(syncState.isCompleted) {
        if (syncState.isCompleted && syncState.hasStartedDownloading) {
            showSyncComplete = true
            delay(3000)
            showSyncComplete = false
            SyncManager.reset()
        }
    }
    
    LaunchedEffect(syncState.isSyncing) {
        if (syncState.isSyncing) {
            showSyncComplete = false
        }
    }
    
    // Listen for real-time Firebase sync events and refresh UI
    LaunchedEffect(Unit) {
        RealtimeSyncService.syncEvents.collectLatest { event ->
            when (event) {
                is RealtimeSyncService.SyncEvent.VideoAdded -> {
                    homeViewModel.refresh()
                    snackbarHostState.showSnackbar("New video added: ${event.title}")
                }
                is RealtimeSyncService.SyncEvent.VideoRemoved -> {
                    homeViewModel.refresh()
                    snackbarHostState.showSnackbar("Video removed: ${event.title}")
                }
                is RealtimeSyncService.SyncEvent.HeroAdded -> {
                    homeViewModel.refresh()
                    snackbarHostState.showSnackbar("New hero video added: ${event.title}")
                }
                is RealtimeSyncService.SyncEvent.HeroRemoved -> {
                    homeViewModel.refresh()
                    snackbarHostState.showSnackbar("Hero video removed: ${event.title}")
                }
                is RealtimeSyncService.SyncEvent.DataChanged -> {
                    homeViewModel.refresh()
                }
            }
        }
    }

    DisposableEffect(stayAwake) {
        if (stayAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var showOfflineBanner by remember { mutableStateOf(false) }
    
    // Battery monitoring
    var batteryLevel by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var showLowBatteryDialog by remember { mutableStateOf(false) }
    val shownBatteryLevels = remember { mutableStateListOf<Int>() }
    val warningLevels = remember { listOf(30, 20, 15, 10, 5) }
    
    // Monitor battery level
    DisposableEffect(Unit) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
                
                if (level != -1 && scale != -1) {
                    val percentage = (level * 100) / scale
                    batteryLevel = percentage
                    
                    if (!isCharging) {
                        for (warningLevel in warningLevels) {
                            if (percentage <= warningLevel && !shownBatteryLevels.contains(warningLevel)) {
                                shownBatteryLevels.add(warningLevel)
                                showLowBatteryDialog = true
                                break
                            }
                        }
                    }
                    
                    if (percentage > 30 || isCharging) {
                        shownBatteryLevels.clear()
                        if (isCharging) {
                            showLowBatteryDialog = false
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        
        onDispose {
            context.unregisterReceiver(batteryReceiver)
        }
    }

    fun isConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Categories,
        BottomNavItem.Settings
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )
    
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isVideoScreen =
        currentRoute?.startsWith("video/") == true ||
                currentRoute?.startsWith("playlist/") == true
    
    var selectedCategoryTab by remember { mutableStateOf("all") }
    
    // Playlist state
    var playlistCategory by remember { mutableStateOf("") }
    var playlistVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var playlistStartIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (!isConnected()) {
            showOfflineBanner = true
            delay(3500)
            showOfflineBanner = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },

        topBar = {
            if (!isVideoScreen) {
                Column {
                    TopBar()

                    AnimatedVisibility(
                        visible = showOfflineBanner,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.no_internet),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = stringResource(R.string.showing_downloaded_videos),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },

        bottomBar = {
            if (!isVideoScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = pagerState.currentPage == index

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.titleRes),
                                    tint = if (selected) AppBlue
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { 
                                Text(stringResource(item.titleRes)) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = AppBlue.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && isConnected() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null && isConnected() -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) + 
                            slideInVertically(
                                initialOffsetY = { it / 20 },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(200))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                targetOffsetY = { it / 20 },
                                animationSpec = tween(200)
                            )
                        }
                    ) {
                        composable("main") {
                            SwipeableTabs(
                                pagerState = pagerState,
                                homeViewModel = homeViewModel,
                                themeViewModel = themeViewModel,
                                settingsViewModel = settingsViewModel,
                                selectedCategoryTab = selectedCategoryTab,
                                onCategoryTabChange = { selectedCategoryTab = it },
                                onVideoClick = { videoId ->
                                    navController.navigate("video/$videoId")
                                },
                                onPlayAll = { category, videos, startIndex ->
                                    playlistCategory = category
                                    playlistVideos = videos
                                    playlistStartIndex = startIndex
                                    navController.navigate("playlist/$category")
                                },
                                isVideoScreenActive = isVideoScreen
                            )
                        }
                        
                        composable("playlist/{category}") {
                            PlaylistScreen(
                                categoryName = playlistCategory,
                                videos = playlistVideos,
                                startIndex = playlistStartIndex,
                                onBack = { navController.popBackStack() },
                                onIncrementView = { videoId -> homeViewModel.incrementView(videoId) }
                            )
                        }

                        composable("video/{videoId}") { backStackEntry ->
                            val videoId = backStackEntry.arguments?.getString("videoId")
                            VideoDetailScreen(
                                videoId = videoId,
                                localVideoPath = null,
                                viewModel = homeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = (syncState.isSyncing && syncState.hasStartedDownloading) || showSyncComplete,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp),
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically { it },
                exit = fadeOut(animationSpec = tween(500)) + slideOutVertically { it }
            ) {
                SmallSyncIndicator(
                    isSyncing = syncState.isSyncing,
                    isComplete = showSyncComplete,
                    progress = syncState.progress,
                    currentItem = actualDownloadedCount,
                    totalItems = syncState.totalItems
                )
            }
        }
    }
    
    if (showLowBatteryDialog) {
        com.hp.hp_omnipad.ui.components.PremiumBatteryWarningDialog(
            batteryLevel = batteryLevel,
            onDismiss = { showLowBatteryDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableTabs(
    pagerState: PagerState,
    homeViewModel: HomeViewModel,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel,
    selectedCategoryTab: String,
    onCategoryTabChange: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onPlayAll: (String, List<VideoItem>, Int) -> Unit,
    isVideoScreenActive: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Internal state to track if we're looking at the privacy policy within the settings tab
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    
    // If the user swipes away from the settings tab, reset the privacy policy view
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 2) {
            showPrivacyPolicy = false
        }
    }
    
    // Handle back button specifically for privacy policy within the tab
    if (showPrivacyPolicy && pagerState.currentPage == 2) {
        BackHandler {
            showPrivacyPolicy = false
        }
    }
    
    val effectivePage = if (pagerState.isScrollInProgress) {
        pagerState.targetPage
    } else {
        pagerState.currentPage
    }
    
    val shouldPauseHeroVideo = effectivePage != 0 || isVideoScreenActive
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true
    ) { page ->
        when (page) {
            0 -> HomeTabScreen(
                viewModel = homeViewModel,
                onVideoClick = onVideoClick,
                onNavigateToLibrary = { categorySlug ->
                    onCategoryTabChange(categorySlug.lowercase())
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                shouldPauseHeroVideo = shouldPauseHeroVideo
            )
            
            1 -> LibraryScreen(
                viewModel = homeViewModel,
                selectedTab = selectedCategoryTab,
                onVideoClick = onVideoClick,
                onPlayAll = onPlayAll,
                onTabSelected = onCategoryTabChange
            )
            
            2 -> {
                if (showPrivacyPolicy) {
                    PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
                } else {
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        viewModel = settingsViewModel,
                        onPrivacyClick = { showPrivacyPolicy = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallSyncIndicator(
    isSyncing: Boolean,
    isComplete: Boolean,
    progress: Float,
    currentItem: Int,
    totalItems: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "sync_progress"
    )
    
    val percentage = (animatedProgress * 100).toInt()
    
    val backgroundColor = when {
        isComplete -> Color(0xFF22C55E)
        else -> Color(0xFF6366F1)
    }

    Card(
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$currentItem/$totalItems",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.ready),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = stringResource(R.string.ready),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}