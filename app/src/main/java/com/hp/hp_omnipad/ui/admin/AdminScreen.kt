package com.hp.hp_omnipad.ui.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hp.hp_omnipad.ui.theme.AppBlue

private val PremiumBlue = Color(0xFF0066CC)
private val PremiumGradientStart = Color(0xFF667eea)
private val PremiumGradientEnd = Color(0xFF764ba2)
private val AccentGold = Color(0xFFFFD700)
private val SurfaceElevated = Color(0xFFF8FAFC)

/*
 * Description: Formats duration in seconds to MM:SS or HH:MM:SS format
 * Params: durationSec - duration in seconds
 * Returns: Formatted duration string
 */
private fun formatDuration(durationSec: Long): String {
    val hours = durationSec / 3600
    val minutes = (durationSec % 3600) / 60
    val seconds = durationSec % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/*
 * Description: Main Admin screen composable that handles login state and displays
 *              either login screen or admin panel based on authentication status
 * Params: viewModel - AdminViewModel instance for state management
 * Returns: Admin UI with login or management panel
 */
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isLoggedIn) {
        AdminPanel(
            viewModel = viewModel,
            onLogout = { viewModel.logout() }
        )
    } else {
        AdminLoginScreen(
            onLogin = { username, password ->
                viewModel.login(username, password)
            },
            isLoading = uiState.isLoading,
            error = uiState.error
        )
    }
}

/*
 * Description: Premium login screen for admin authentication
 * Params: onLogin - callback with username and password, isLoading - loading state, error - error message
 * Returns: Premium styled login form UI
 */
@Composable
private fun AdminLoginScreen(
    onLogin: (String, String) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PremiumGradientStart.copy(alpha = 0.1f),
                        PremiumGradientEnd.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = PremiumBlue.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium logo/icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PremiumGradientStart, PremiumGradientEnd)
                            ),
                            CircleShape
                        )
                        .shadow(12.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Text(
                    text = "Admin Portal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Sign in to manage your content",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = PremiumBlue
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumBlue,
                        focusedLabelColor = PremiumBlue
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = PremiumBlue
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumBlue,
                        focusedLabelColor = PremiumBlue
                    )
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sign in button
                Button(
                    onClick = { onLogin(username, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = username.isNotBlank() && password.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumBlue,
                        disabledContainerColor = PremiumBlue.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Sign In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Security notice
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = PremiumBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Secure admin access with encrypted connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/*
 * Description: Premium admin panel for managing videos after successful login
 * Params: viewModel - AdminViewModel instance, onLogout - logout callback
 * Returns: Video management UI with tabs for Heroes and Videos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPanel(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Hero Videos", "Normal Videos", "Support Tickets")
    
    LaunchedEffect(Unit) {
        viewModel.loadVideos()
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Premium header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        PremiumGradientStart.copy(alpha = 0.1f),
                                        PremiumGradientEnd.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PremiumGradientStart, PremiumGradientEnd)
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Online • ${uiState.videos.size + uiState.heroes.size} items • ${uiState.supportTickets.size} tickets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        FilledTonalButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Logout",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.Star,
                            label = "Heroes",
                            value = "${uiState.heroes.size}",
                            color = AccentGold,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Default.VideoLibrary,
                            label = "Videos",
                            value = "${uiState.videos.size}",
                            color = PremiumBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Default.CheckCircle,
                            label = "Published",
                            value = "${uiState.videos.count { it.published }}",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Tab bar
            Surface(
                color = MaterialTheme.colorScheme.surface
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PremiumBlue,
                    divider = {} // Remove the default divider line
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (index) {
                                            0 -> Icons.Default.Star
                                            1 -> Icons.Default.VideoLibrary
                                            else -> Icons.Default.SupportAgent
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selectedTab == index) PremiumBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == index) PremiumBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Custom indicator
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (selectedTab == index) PremiumBlue else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> HeroVideosTab(
                    heroes = uiState.heroes,
                    isLoading = uiState.isLoading,
                    onEdit = { viewModel.showEditHeroDialog(it) },
                    onDelete = { viewModel.deleteHero(it) },
                    onAdd = { viewModel.showAddHeroDialog() }
                )
                1 -> NormalVideosTab(
                    videos = uiState.videos,
                    isLoading = uiState.isLoading,
                    onEdit = { viewModel.showEditVideoDialog(it) },
                    onDelete = { viewModel.deleteVideo(it) },
                    onAdd = { viewModel.showAddVideoDialog() }
                )
                2 -> SupportTicketsTab(
                    tickets = uiState.supportTickets,
                    isLoading = uiState.isLoading,
                    onUpdateStatus = { ticketId, status -> viewModel.updateTicketStatus(ticketId, status) },
                    onDelete = { ticketId -> viewModel.deleteTicket(ticketId) }
                )
            }
        }
    }
    
    // Edit/Add dialogs
    if (uiState.showVideoDialog) {
        VideoEditDialog(
            video = uiState.editingVideo,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { viewModel.saveVideo(it) }
        )
    }
    
    if (uiState.showHeroDialog) {
        HeroEditDialog(
            hero = uiState.editingHero,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { viewModel.saveHero(it) }
        )
    }
}

/*
 * Description: Compact stat card for dashboard header
 * Params: icon - display icon, label - stat name, value - stat value, color - accent color
 * Returns: Compact stat display card
 */
@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/*
 * Description: Tab content for managing hero videos
 * Params: heroes - list of hero videos, isLoading - loading state, callbacks for actions
 * Returns: List of hero videos with edit/delete options
 */
@Composable
private fun HeroVideosTab(
    heroes: List<HeroData>,
    isLoading: Boolean,
    onEdit: (HeroData) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = PremiumBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading heroes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (heroes.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Star,
                title = "No Hero Videos",
                subtitle = "Add your first hero video to get started"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(heroes) { hero ->
                    PremiumVideoCard(
                        title = hero.title,
                        thumbnailUrl = hero.thumbnailUrl,
                        status = if (hero.active) "Active" else "Inactive",
                        statusColor = if (hero.active) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        onEdit = { onEdit(hero) },
                        onDelete = { onDelete(hero.id) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        
        // FAB
        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = PremiumBlue,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Hero", fontWeight = FontWeight.SemiBold)
        }
    }
}

/*
 * Description: Tab content for managing normal videos
 * Params: videos - list of videos, isLoading - loading state, callbacks for actions
 * Returns: List of videos with edit/delete options
 */
@Composable
private fun NormalVideosTab(
    videos: List<VideoData>,
    isLoading: Boolean,
    onEdit: (VideoData) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = PremiumBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading videos...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (videos.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.VideoLibrary,
                title = "No Videos",
                subtitle = "Add your first video to get started"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(videos) { video ->
                    val durationFormatted = formatDuration(video.durationSec)
                    PremiumVideoCard(
                        title = video.title,
                        thumbnailUrl = video.thumbnailUrl,
                        status = if (video.published) "Published" else "Draft",
                        statusColor = if (video.published) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        extraInfo = "${video.viewCount} views • $durationFormatted",
                        onEdit = { onEdit(video) },
                        onDelete = { onDelete(video.id) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        
        // FAB
        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = PremiumBlue,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Video", fontWeight = FontWeight.SemiBold)
        }
    }
}

/*
 * Description: Support Tickets tab showing all user submitted tickets
 * Params: tickets - list of support tickets, isLoading - loading state, callbacks for actions
 * Returns: List of support tickets with status management
 */
@Composable
private fun SupportTicketsTab(
    tickets: List<SupportTicket>,
    isLoading: Boolean,
    onUpdateStatus: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = PremiumBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading tickets...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (tickets.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.SupportAgent,
                title = "No Support Tickets",
                subtitle = "Support tickets from users will appear here"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tickets) { ticket ->
                    SupportTicketCard(
                        ticket = ticket,
                        onUpdateStatus = { newStatus -> onUpdateStatus(ticket.id, newStatus) },
                        onDelete = { onDelete(ticket.id) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

/*
 * Description: Premium styled support ticket card
 * Params: ticket - SupportTicket data, onUpdateStatus - status change callback, onDelete - delete callback
 * Returns: Card UI with ticket info, message preview, and action buttons
 */
@Composable
private fun SupportTicketCard(
    ticket: SupportTicket,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val statusColor = when (ticket.status) {
        "OPEN" -> Color(0xFF2196F3)
        "IN_PROGRESS" -> Color(0xFFFF9800)
        "RESOLVED" -> Color(0xFF4CAF50)
        "CLOSED" -> Color(0xFF9E9E9E)
        else -> Color(0xFF2196F3)
    }
    
    val priorityColor = when (ticket.priority) {
        "HIGH" -> Color(0xFFF44336)
        "MEDIUM" -> Color(0xFFFF9800)
        "LOW" -> Color(0xFF4CAF50)
        else -> Color(0xFF9E9E9E)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (ticket.priority) {
                            "HIGH" -> Icons.Default.PriorityHigh
                            "MEDIUM" -> Icons.Default.Remove
                            else -> Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ticket.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Email,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ticket.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Status badge
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = ticket.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category and date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PremiumBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = ticket.category.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PremiumBlue
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    Icons.Default.Schedule,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = ticket.createdAtFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Message preview/full
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = ticket.message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Actions when expanded
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Update Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "OPEN" to "Open",
                        "IN_PROGRESS" to "Progress",
                        "RESOLVED" to "Resolved",
                        "CLOSED" to "Closed"
                    ).forEach { (status, label) ->
                        val isSelected = ticket.status == status
                        val color = when (status) {
                            "OPEN" -> Color(0xFF2196F3)
                            "IN_PROGRESS" -> Color(0xFFFF9800)
                            "RESOLVED" -> Color(0xFF4CAF50)
                            "CLOSED" -> Color(0xFF9E9E9E)
                            else -> Color.Gray
                        }
                        
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 2.dp,
                                        color = color,
                                        shape = RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable { onUpdateStatus(status) },
                            color = if (isSelected) color else color.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else color,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Delete button
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Ticket")
                }
            }
            
            // Expand indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Delete Ticket?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This action cannot be undone. The ticket from ${ticket.name} will be permanently deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/*
 * Description: Premium styled video card with thumbnail
 * Params: title - video title, thumbnailUrl - image URL, status - publish state, callbacks
 * Returns: Premium card UI with video info and action buttons
 */
@Composable
private fun PremiumVideoCard(
    title: String,
    thumbnailUrl: String,
    status: String,
    statusColor: Color,
    extraInfo: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Larger Thumbnail (increased from 80dp to 120dp x 80dp for 16:9 aspect)
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shadow(4.dp, RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                if (extraInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons - larger sizes
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledTonalIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = PremiumBlue.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PremiumBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FilledTonalIconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Delete Video?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to delete:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "\"$title\"",
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/*
 * Description: Empty state card when no items exist
 * Params: icon - display icon, title - main message, subtitle - secondary message
 * Returns: Centered empty state UI
 */
@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
