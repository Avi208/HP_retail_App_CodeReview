# Omni Pad - Technical Documentation

This document provides in-depth technical details for developers working on the Omni Pad application.

---

## Table of Contents

1. [Application Entry Point](#application-entry-point)
2. [Navigation Architecture](#navigation-architecture)
3. [State Management](#state-management)
4. [Data Flow](#data-flow)
5. [Video Playback System](#video-playback-system)
6. [Offline Sync System](#offline-sync-system)
7. [Firebase Integration](#firebase-integration)
8. [UI Components](#ui-components)
9. [Settings System](#settings-system)
10. [Admin System](#admin-system)

---

## Application Entry Point

### MainActivity.kt

The single activity serves as the entry point and handles:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Apply language settings
        applyLanguageSettings()
        
        // 2. Set up edge-to-edge display
        enableEdgeToEdge()
        
        // 3. Initialize sync services
        initializeSyncServices()
        
        // 4. Set content with Compose
        setContent {
            OmniPadTheme {
                HomeScreen(themeViewModel)
            }
        }
    }
}
```

### HpOmnipadApplication.kt

Application-level initialization:

```kotlin
class HpOmnipadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize VideoSyncManager with application context
        VideoSyncManager.initialize(this)
    }
}
```

---

## Navigation Architecture

### Bottom Navigation

Four main tabs managed via `HorizontalPager`:

```kotlin
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Categories : BottomNavItem("categories", "Categories", Icons.Default.Category)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
    object Admin : BottomNavItem("admin", "Admin", Icons.Default.AdminPanelSettings)
}
```

### NavHost Routes

```kotlin
NavHost(navController, startDestination = "main") {
    composable("main") { /* SwipeableTabs */ }
    composable("video/{videoId}") { VideoDetailScreen(...) }
    composable("playlist/{category}") { PlaylistScreen(...) }
    composable("whats_new") { WhatsNewScreen(...) }
    composable("privacy_policy") { PrivacyPolicyScreen(...) }
    composable("contact_support") { ContactSupportScreen(...) }
}
```

### Swipe Navigation

Uses `HorizontalPager` with `PagerState`:

```kotlin
val pagerState = rememberPagerState(
    initialPage = 0,
    pageCount = { 4 }
)

HorizontalPager(state = pagerState) { page ->
    when (page) {
        0 -> HomeTabScreen(...)
        1 -> LibraryScreen(...)
        2 -> SettingsScreen(...)
        3 -> AdminScreen(...)
    }
}
```

---

## State Management

### StateFlow Pattern

ViewModels expose state via `StateFlow`:

```kotlin
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun updateState(update: (HomeUiState) -> HomeUiState) {
        _uiState.update(update)
    }
}
```

### UI State Classes

```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null
)
```

### Collecting State in Composables

```kotlin
@Composable
fun HomeTabScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorMessage(uiState.error)
        else -> ContentList(uiState.videos)
    }
}
```

---

## Data Flow

### Repository Pattern

```
┌──────────────┐      ┌────────────────┐      ┌─────────────┐
│  Composable  │ ──── │   ViewModel    │ ──── │ Repository  │
│              │      │                │      │             │
│ collectState │      │ StateFlow      │      │ Firestore   │
│              │      │ viewModelScope │      │ Local Cache │
└──────────────┘      └────────────────┘      └─────────────┘
```

### FirestoreRepository

```kotlin
class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val videosCollection = db.collection("videos")
    
    suspend fun getVideos(): Result<List<VideoItem>> {
        return try {
            val snapshot = videosCollection
                .whereEqualTo("published", true)
                .get()
                .await()
            Result.success(snapshot.toObjects())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Local Caching

Videos metadata cached as JSON:

```kotlin
object VideoSyncManager {
    fun cacheVideoMetadata(video: VideoItem) {
        val json = Gson().toJson(video)
        File(getCacheDir(), "${video.id}.json").writeText(json)
    }
    
    fun getCachedVideos(): List<VideoItem> {
        return getCacheDir().listFiles()
            ?.filter { it.extension == "json" }
            ?.map { Gson().fromJson(it.readText(), VideoItem::class.java) }
            ?: emptyList()
    }
}
```

---

## Video Playback System

### VideoPlayerViewModel

Manages ExoPlayer instance and playback state:

```kotlin
class VideoPlayerViewModel : ViewModel() {
    val exoPlayer: ExoPlayer
    
    private val _availableQualities = MutableStateFlow<List<String>>(emptyList())
    val availableQualities: StateFlow<List<String>> = _availableQualities
    
    private val _selectedQuality = MutableStateFlow("Auto")
    val selectedQuality: StateFlow<String> = _selectedQuality
    
    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(buildUponParameters().setMaxVideoSizeSd())
    }
    
    fun prepare(videoUrl: String, autoPlay: Boolean = true) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay
    }
    
    fun setVideoQuality(quality: String) {
        _selectedQuality.value = quality
        when (quality) {
            "Auto" -> trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .clearVideoSizeConstraints()
            )
            else -> {
                val height = quality.replace("p", "").toInt()
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setMaxVideoSize(Int.MAX_VALUE, height)
                )
            }
        }
    }
}
```

### VideoPlayer Composable

```kotlin
@Composable
fun VideoPlayer(
    videoUrl: String,
    exoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    onFullScreenToggle: () -> Unit,
    availableQualities: List<String>,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // Custom controls
            }
        },
        modifier = modifier
    )
    
    // Custom controls overlay
    VideoControls(
        isPlaying = exoPlayer.isPlaying,
        onPlayPause = { /* toggle */ },
        currentPosition = exoPlayer.currentPosition,
        duration = exoPlayer.duration,
        onSeek = { exoPlayer.seekTo(it) },
        // ... other controls
    )
}
```

### Captions System

```kotlin
data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String
)

fun selectSubtitle(track: SubtitleTrack) {
    val trackGroups = exoPlayer.currentTracks.groups
    trackGroups.forEach { group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            // Find and select matching track
            val override = TrackSelectionOverride(group.mediaTrackGroup, 0)
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setOverrideForType(override)
            )
        }
    }
}
```

---

## Offline Sync System

### VideoSyncManager

Central manager for offline video storage:

```kotlin
object VideoSyncManager {
    private lateinit var appContext: Context
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    // Storage path
    private fun getBaseDir(): File {
        val moviesDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File(moviesDir, "OmniPad")
    }
    
    // Check if video is downloaded
    fun isVideoDownloaded(videoId: String): Boolean {
        val videoDir = File(getBaseDir(), videoId)
        val videoFile = File(videoDir, "video.mp4")
        return videoFile.exists()
    }
    
    // Get local video path
    fun getLocalVideoPath(videoId: String): String? {
        val videoFile = File(getBaseDir(), "$videoId/video.mp4")
        return if (videoFile.exists()) videoFile.absolutePath else null
    }
    
    // Download video
    suspend fun downloadVideo(video: VideoItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val videoDir = File(getBaseDir(), video.id)
                videoDir.mkdirs()
                
                // Download video file
                downloadFile(video.videoUrl, File(videoDir, "video.mp4"))
                
                // Download thumbnail
                downloadFile(video.thumbnailUrl, File(videoDir, "thumbnail.jpg"))
                
                // Save metadata
                saveMetadata(video, File(videoDir, "metadata.json"))
                
                true
            } catch (e: Exception) {
                Log.e("VideoSyncManager", "Download failed: ${e.message}")
                false
            }
        }
    }
}
```

### SyncManager

Tracks sync progress state:

```kotlin
object SyncManager {
    data class SyncState(
        val isSyncing: Boolean = false,
        val isCompleted: Boolean = false,
        val hasStartedDownloading: Boolean = false,
        val progress: Float = 0f,
        val completedItems: Int = 0,
        val totalItems: Int = 0
    )
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    fun startSync(totalItems: Int) {
        _syncState.value = SyncState(
            isSyncing = true,
            hasStartedDownloading = true,
            totalItems = totalItems
        )
    }
    
    fun updateProgress(completed: Int) {
        _syncState.update { current ->
            current.copy(
                completedItems = completed,
                progress = completed.toFloat() / current.totalItems
            )
        }
    }
}
```

### RealtimeSyncService

Firebase snapshot listeners for real-time updates:

```kotlin
object RealtimeSyncService {
    sealed class SyncEvent {
        data class VideoAdded(val video: VideoItem) : SyncEvent()
        data class VideoRemoved(val videoId: String) : SyncEvent()
        data class VideoUpdated(val video: VideoItem) : SyncEvent()
    }
    
    private val _syncEvents = MutableSharedFlow<SyncEvent>()
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()
    
    fun startListening() {
        db.collection("videos")
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            _syncEvents.tryEmit(SyncEvent.VideoAdded(...))
                        }
                        DocumentChange.Type.REMOVED -> {
                            _syncEvents.tryEmit(SyncEvent.VideoRemoved(...))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            _syncEvents.tryEmit(SyncEvent.VideoUpdated(...))
                        }
                    }
                }
            }
    }
}
```

---

## Firebase Integration

### Data Models

```kotlin
// Video data from Firestore
data class VideoData(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val durationSec: Long = 0,
    val viewCount: Int = 0,
    val published: Boolean = true,
    val categoryIds: List<String> = emptyList(),
    val language: String = "en",
    val tags: List<String> = emptyList()
)

// Hero video from Firestore
data class HeroData(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val active: Boolean = true,
    val order: Int = 0
)

// Support ticket
data class SupportTicket(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val category: String = "",
    val priority: String = "",
    val status: String = "OPEN",
    val createdAt: Long = 0,
    val createdAtFormatted: String = ""
)
```

### Firestore Operations

```kotlin
// Read with filtering
suspend fun getPublishedVideos(): List<VideoData> {
    return db.collection("videos")
        .whereEqualTo("published", true)
        .get()
        .await()
        .toObjects(VideoData::class.java)
}

// Write/Update
suspend fun updateVideo(id: String, data: Map<String, Any>) {
    db.collection("videos")
        .document(id)
        .update(data)
        .await()
}

// Increment view count
suspend fun incrementViewCount(videoId: String) {
    db.collection("videos")
        .document(videoId)
        .update("viewCount", FieldValue.increment(1))
        .await()
}

// Create support ticket
suspend fun createTicket(ticket: SupportTicket) {
    db.collection("support_tickets")
        .add(ticket)
        .await()
}
```

---

## UI Components

### VideoCard

Reusable video thumbnail card with press animation:

```kotlin
@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource, indication = null) { onClick() }
    ) {
        // Thumbnail
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop
        )
        
        // Duration badge
        DurationBadge(video.duration)
        
        // Title and views
        VideoInfo(video.title, video.views)
    }
}
```

### PremiumDialogs

Reusable premium-styled dialogs:

```kotlin
@Composable
fun PremiumBatteryWarningDialog(
    batteryLevel: Int,
    onDismiss: () -> Unit
) {
    // Animated circular battery indicator
    // Pulsing glow effect
    // Color-coded by battery level
    // Contextual warning text
}

@Composable
fun PremiumConfirmationDialog(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Bouncy scale-in animation
    // Themed icon with background
    // Action buttons
}
```

---

## Settings System

### SettingsViewModel

```kotlin
class SettingsViewModel : ViewModel() {
    private val prefs: SharedPreferences
    
    // Push notifications
    private val _pushNotificationsEnabled = MutableStateFlow(true)
    val pushNotificationsEnabled: StateFlow<Boolean>
    
    // Language
    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String>
    
    // Stay awake
    private val _stayAwakeEnabled = MutableStateFlow(false)
    val stayAwakeEnabled: StateFlow<Boolean>
    
    // Offload app (offline mode)
    private val _offloadAppEnabled = MutableStateFlow(true)
    val offloadAppEnabled: StateFlow<Boolean>
    
    fun setLanguage(langCode: String) {
        _selectedLanguage.value = langCode
        prefs.edit().putString("language", langCode).apply()
        _languageChanged.value = true // Triggers activity recreation
    }
    
    fun toggleOffloadApp() {
        val newValue = !_offloadAppEnabled.value
        _offloadAppEnabled.value = newValue
        
        if (!newValue) {
            // Delete all downloaded videos
            viewModelScope.launch {
                VideoSyncManager.deleteAllDownloads()
            }
        } else {
            // Start downloading all videos
            viewModelScope.launch {
                VideoSyncManager.syncAllVideos()
            }
        }
    }
}
```

### Preferences Storage

Using SharedPreferences:

```kotlin
private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

// Save preference
prefs.edit().putBoolean("dark_theme", isDark).apply()

// Read preference
val isDark = prefs.getBoolean("dark_theme", false)
```

---

## Admin System

### AdminViewModel

```kotlin
class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    // Hardcoded credentials (use Firebase Auth in production)
    private val ADMIN_USERNAME = "admin"
    private val ADMIN_PASSWORD = "admin123"
    
    fun login(username: String, password: String) {
        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            _uiState.update { it.copy(isLoggedIn = true) }
            loadVideos()
        } else {
            _uiState.update { it.copy(error = "Invalid credentials") }
        }
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            // Load videos, heroes, and support tickets
            val videos = db.collection("videos").get().await()
            val heroes = db.collection("heroes").get().await()
            val tickets = db.collection("support_tickets").get().await()
            
            _uiState.update {
                it.copy(
                    videos = videos.toObjects(),
                    heroes = heroes.toObjects(),
                    supportTickets = tickets.toObjects()
                )
            }
        }
    }
    
    fun saveVideo(video: VideoData) {
        viewModelScope.launch {
            val data = mapOf(
                "title" to video.title,
                "videoUrl" to video.videoUrl,
                // ... other fields
            )
            
            if (video.id.isNotEmpty()) {
                db.collection("videos").document(video.id).set(data).await()
            } else {
                db.collection("videos").document(video.title).set(data).await()
            }
            
            loadVideos()
        }
    }
    
    fun updateTicketStatus(ticketId: String, newStatus: String) {
        viewModelScope.launch {
            db.collection("support_tickets")
                .document(ticketId)
                .update("status", newStatus)
                .await()
            loadVideos()
        }
    }
}
```

### Admin UI Structure

```
AdminScreen
├── AdminLoginScreen (when not logged in)
│   ├── Username field
│   ├── Password field
│   └── Login button
│
└── AdminPanel (when logged in)
    ├── Header with stats
    ├── TabRow
    │   ├── Hero Videos tab
    │   ├── Normal Videos tab
    │   └── Support Tickets tab
    │
    ├── HeroVideosTab
    │   ├── LazyColumn of PremiumVideoCards
    │   └── FAB to add new hero
    │
    ├── NormalVideosTab
    │   ├── LazyColumn of PremiumVideoCards
    │   └── FAB to add new video
    │
    └── SupportTicketsTab
        └── LazyColumn of SupportTicketCards
            └── Expandable with status controls
```

---

## Performance Considerations

### Image Loading

Using Coil with caching:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(thumbnailUrl)
        .crossfade(true)
        .memoryCacheKey(videoId)
        .diskCacheKey(videoId)
        .build(),
    contentDescription = title
)
```

### LazyColumn Optimization

```kotlin
LazyColumn {
    items(
        items = videos,
        key = { it.id } // Stable keys for recomposition
    ) { video ->
        VideoCard(video)
    }
}
```

### Remember Expensive Calculations

```kotlin
val sortedVideos = remember(videos) {
    videos.sortedByDescending { it.viewCount }
}
```

---

## Error Handling

### Network Errors

```kotlin
try {
    val result = repository.getVideos()
    _uiState.update { it.copy(videos = result, error = null) }
} catch (e: IOException) {
    // Network error - show cached data
    val cached = VideoSyncManager.getCachedVideos()
    _uiState.update { it.copy(videos = cached, error = "Offline mode") }
} catch (e: Exception) {
    _uiState.update { it.copy(error = e.message) }
}
```

### Firebase Errors

```kotlin
db.collection("videos")
    .addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.e("Firebase", "Listen failed: ${error.message}")
            return@addSnapshotListener
        }
        // Process snapshot
    }
```

---

## Testing Guidelines

### Unit Tests

```kotlin
class HomeViewModelTest {
    @Test
    fun `loadVideos updates state correctly`() = runTest {
        val viewModel = HomeViewModel(FakeRepository())
        viewModel.loadVideos()
        
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(3, viewModel.uiState.value.videos.size)
    }
}
```

### UI Tests

```kotlin
@Test
fun videoCard_displaysCorrectInfo() {
    composeTestRule.setContent {
        VideoCard(
            video = testVideo,
            onClick = {}
        )
    }
    
    composeTestRule.onNodeWithText("Test Video").assertIsDisplayed()
    composeTestRule.onNodeWithText("1.5K views").assertIsDisplayed()
}
```

---

## Future Improvements

1. **Firebase Authentication** - Replace hardcoded admin credentials
2. **Hilt Dependency Injection** - Better testability and modularity
3. **Room Database** - More robust local caching
4. **WorkManager** - Background sync scheduling
5. **Push Notifications** - Firebase Cloud Messaging integration
6. **Analytics** - Firebase Analytics for usage tracking
7. **Crashlytics** - Crash reporting
8. **ProGuard/R8** - Code obfuscation for release builds
