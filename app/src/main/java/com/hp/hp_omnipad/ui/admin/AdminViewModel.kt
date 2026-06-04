package com.hp.hp_omnipad.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/*
 * Description: Data class representing a video in the admin panel
 * Params: id - document ID, title - video title, and other video properties
 * Returns: VideoData instance
 */
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

/*
 * Description: Data class representing a hero video in the admin panel
 * Params: id - document ID, title - hero title, and other hero properties
 * Returns: HeroData instance
 */
data class HeroData(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val active: Boolean = true,
    val order: Int = 0
)

/*
 * Description: Data class representing a support ticket
 * Params: id - document ID, name - user name, email, subject, message, category, priority, status, createdAt
 * Returns: SupportTicket instance
 */
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

/*
 * Description: UI state for the admin screen
 * Params: Various state properties for login, loading, videos, heroes, dialogs
 * Returns: AdminUiState instance
 */
data class AdminUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val videos: List<VideoData> = emptyList(),
    val heroes: List<HeroData> = emptyList(),
    val supportTickets: List<SupportTicket> = emptyList(),
    val showVideoDialog: Boolean = false,
    val showHeroDialog: Boolean = false,
    val editingVideo: VideoData? = null,
    val editingHero: HeroData? = null
)

/*
 * Description: ViewModel for admin panel managing authentication and video CRUD operations
 * Params: None
 * Returns: AdminViewModel instance with state and methods for admin operations
 */
class AdminViewModel : ViewModel() {
    
    private val TAG = "AdminViewModel"
    private val db = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
    
    // Admin credentials - In production, use Firebase Auth
    private val ADMIN_USERNAME = "HP"
    private val ADMIN_PASSWORD = "HP"
    
    /*
     * Description: Authenticates admin user with username and password
     * Params: username - admin username, password - admin password
     * Returns: Unit - updates UI state with login result
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Simple credential check - In production, use Firebase Auth
            if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    isLoading = false
                )
                loadVideos()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Invalid username or password"
                )
            }
        }
    }
    
    /*
     * Description: Logs out the admin user
     * Params: None
     * Returns: Unit - resets UI state to logged out
     */
    fun logout() {
        _uiState.value = AdminUiState()
    }
    
    /*
     * Description: Loads all videos and heroes from Firebase
     * Params: None
     * Returns: Unit - updates UI state with fetched data
     */
    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load videos
                val videosSnapshot = db.collection("videos").get().await()
                val videos = videosSnapshot.documents.mapNotNull { doc ->
                    try {
                        VideoData(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            videoUrl = doc.getString("videoUrl") ?: "",
                            thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                            durationSec = doc.getLong("durationSec") ?: 0,
                            viewCount = (doc.getLong("viewCount") ?: 0).toInt(),
                            published = doc.getBoolean("published") ?: true,
                            categoryIds = (doc.get("categoryIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            language = doc.getString("language") ?: "en",
                            tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing video: ${e.message}")
                        null
                    }
                }
                
                // Load heroes
                val heroesSnapshot = db.collection("heroes").get().await()
                val heroes = heroesSnapshot.documents.mapNotNull { doc ->
                    try {
                        HeroData(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            videoUrl = doc.getString("videoUrl") ?: "",
                            thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                            active = doc.getBoolean("active") ?: true,
                            order = (doc.getLong("order") ?: 0).toInt()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing hero: ${e.message}")
                        null
                    }
                }
                
                // Load support tickets
                val ticketsSnapshot = db.collection("support_tickets").get().await()
                val tickets = ticketsSnapshot.documents.mapNotNull { doc ->
                    try {
                        SupportTicket(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            subject = doc.getString("subject") ?: "",
                            message = doc.getString("message") ?: "",
                            category = doc.getString("category") ?: "",
                            priority = doc.getString("priority") ?: "",
                            status = doc.getString("status") ?: "OPEN",
                            createdAt = doc.getLong("createdAt") ?: 0,
                            createdAtFormatted = doc.getString("createdAtFormatted") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing ticket: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.createdAt }
                
                _uiState.value = _uiState.value.copy(
                    videos = videos,
                    heroes = heroes.sortedBy { it.order },
                    supportTickets = tickets,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading videos: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load videos"
                )
            }
        }
    }
    
    /*
     * Description: Shows the video edit dialog for a specific video
     * Params: video - VideoData to edit
     * Returns: Unit - updates UI state to show dialog
     */
    fun showEditVideoDialog(video: VideoData) {
        _uiState.value = _uiState.value.copy(
            showVideoDialog = true,
            editingVideo = video
        )
    }
    
    /*
     * Description: Shows the video add dialog for creating new video
     * Params: None
     * Returns: Unit - updates UI state to show dialog
     */
    fun showAddVideoDialog() {
        _uiState.value = _uiState.value.copy(
            showVideoDialog = true,
            editingVideo = null
        )
    }
    
    /*
     * Description: Shows the hero edit dialog for a specific hero
     * Params: hero - HeroData to edit
     * Returns: Unit - updates UI state to show dialog
     */
    fun showEditHeroDialog(hero: HeroData) {
        _uiState.value = _uiState.value.copy(
            showHeroDialog = true,
            editingHero = hero
        )
    }
    
    /*
     * Description: Shows the hero add dialog for creating new hero
     * Params: None
     * Returns: Unit - updates UI state to show dialog
     */
    fun showAddHeroDialog() {
        _uiState.value = _uiState.value.copy(
            showHeroDialog = true,
            editingHero = null
        )
    }
    
    /*
     * Description: Dismisses any open dialog
     * Params: None
     * Returns: Unit - updates UI state to hide dialogs
     */
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showVideoDialog = false,
            showHeroDialog = false,
            editingVideo = null,
            editingHero = null
        )
    }
    
    /*
     * Description: Saves a video (create or update) to Firebase
     * Params: video - VideoData to save
     * Returns: Unit - updates Firebase and refreshes list
     */
    fun saveVideo(video: VideoData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val data = hashMapOf(
                    "title" to video.title,
                    "description" to video.description,
                    "videoUrl" to video.videoUrl,
                    "thumbnailUrl" to video.thumbnailUrl,
                    "durationSec" to video.durationSec,
                    "viewCount" to video.viewCount,
                    "published" to video.published,
                    "categoryIds" to video.categoryIds,
                    "language" to video.language,
                    "tags" to video.tags,
                    "addedAt" to (System.currentTimeMillis() / 1000)
                )
                
                if (video.id.isNotEmpty()) {
                    // Update existing
                    db.collection("videos").document(video.id).set(data).await()
                } else {
                    // Create new
                    db.collection("videos").add(data).await()
                }
                
                dismissDialog()
                loadVideos()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving video: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save video"
                )
            }
        }
    }
    
    /*
     * Description: Deletes a video from Firebase
     * Params: videoId - ID of video to delete
     * Returns: Unit - removes from Firebase and refreshes list
     */
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            try {
                db.collection("videos").document(videoId).delete().await()
                loadVideos()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting video: ${e.message}")
            }
        }
    }
    
    /*
     * Description: Saves a hero video (create or update) to Firebase
     * Params: hero - HeroData to save
     * Returns: Unit - updates Firebase and refreshes list
     */
    fun saveHero(hero: HeroData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val data = hashMapOf(
                    "title" to hero.title,
                    "videoUrl" to hero.videoUrl,
                    "thumbnailUrl" to hero.thumbnailUrl,
                    "active" to hero.active,
                    "order" to hero.order
                )
                
                if (hero.id.isNotEmpty()) {
                    db.collection("heroes").document(hero.id).set(data).await()
                } else {
                    db.collection("heroes").add(data).await()
                }
                
                dismissDialog()
                loadVideos()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving hero: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save hero"
                )
            }
        }
    }
    
    /*
     * Description: Deletes a hero video from Firebase
     * Params: heroId - ID of hero to delete
     * Returns: Unit - removes from Firebase and refreshes list
     */
    fun deleteHero(heroId: String) {
        viewModelScope.launch {
            try {
                db.collection("heroes").document(heroId).delete().await()
                loadVideos()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting hero: ${e.message}")
            }
        }
    }
    
    /*
     * Description: Updates the status of a support ticket
     * Params: ticketId - ID of ticket, newStatus - new status value
     * Returns: Unit - updates Firebase and refreshes list
     */
    fun updateTicketStatus(ticketId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                db.collection("support_tickets").document(ticketId)
                    .update("status", newStatus)
                    .await()
                loadVideos()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ticket status: ${e.message}")
            }
        }
    }
    
    /*
     * Description: Deletes a support ticket from Firebase
     * Params: ticketId - ID of ticket to delete
     * Returns: Unit - removes from Firebase and refreshes list
     */
    fun deleteTicket(ticketId: String) {
        viewModelScope.launch {
            try {
                db.collection("support_tickets").document(ticketId).delete().await()
                loadVideos()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ticket: ${e.message}")
            }
        }
    }
}