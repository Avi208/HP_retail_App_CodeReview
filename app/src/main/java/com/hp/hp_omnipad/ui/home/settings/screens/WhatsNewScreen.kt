package com.hp.hp_omnipad.ui.home.settings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val PremiumBlue = Color(0xFF0096D6)
private val PremiumGradientStart = Color(0xFF0096D6)
private val PremiumGradientEnd = Color(0xFF00D4AA)
private val FeatureGreen = Color(0xFF4CAF50)
private val BugFixOrange = Color(0xFFFF9800)
private val ImprovementPurple = Color(0xFF9C27B0)

data class UpdateItem(
    val version: String,
    val date: String,
    val items: List<ChangeItem>
)

data class ChangeItem(
    val type: ChangeType,
    val title: String,
    val description: String
)

enum class ChangeType {
    FEATURE, BUG_FIX, IMPROVEMENT
}

/*
 * Description: Premium What's New screen showing recent updates and changes
 * Params: onBack - callback to navigate back
 * Returns: Scrollable list of version updates with animated entries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(onBack: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    val updates = remember {
        listOf(
            UpdateItem(
                version = "1.0.0",
                date = "March 2026",
                items = listOf(
                    ChangeItem(ChangeType.FEATURE, "Playlist Mode", "Play all videos in a category with premium playlist UI"),
                    ChangeItem(ChangeType.FEATURE, "Offline Mode", "Download videos for offline viewing with Offload App toggle"),
                    ChangeItem(ChangeType.FEATURE, "Captions Support", "CC button with English and Hindi subtitle support"),
                    ChangeItem(ChangeType.FEATURE, "Contact Support", "Support ticket system with Firebase integration"),
                    ChangeItem(ChangeType.FEATURE, "Adaptive Bitrate", "Automatic video quality adjustment based on network speed"),
                    ChangeItem(ChangeType.FEATURE, "Dark Theme", "Beautiful dark mode support throughout the app"),
                    ChangeItem(ChangeType.IMPROVEMENT, "Premium UI", "Smooth animations and premium design throughout the app")
                )
            )
        )
    }
    
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column {
                    // Gradient header
                    Box(
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
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "What's New",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Latest features and improvements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Sparkle icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(PremiumGradientStart, PremiumGradientEnd)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(updates) { versionIndex, update ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = versionIndex * 150)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                ) {
                    VersionCard(update)
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun VersionCard(update: UpdateItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Version header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Version badge
                Surface(
                    color = PremiumBlue,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "v${update.version}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = update.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Change count
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${update.items.size} changes",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Change items
            update.items.forEachIndexed { index, item ->
                ChangeItemCard(item)
                if (index < update.items.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ChangeItemCard(item: ChangeItem) {
    val (icon, color, label) = when (item.type) {
        ChangeType.FEATURE -> Triple(Icons.Default.NewReleases, FeatureGreen, "NEW")
        ChangeType.BUG_FIX -> Triple(Icons.Default.BugReport, BugFixOrange, "FIX")
        ChangeType.IMPROVEMENT -> Triple(Icons.Default.TrendingUp, ImprovementPurple, "IMPROVED")
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 9.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
