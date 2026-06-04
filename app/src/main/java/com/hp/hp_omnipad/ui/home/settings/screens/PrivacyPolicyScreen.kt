package com.hp.hp_omnipad.ui.home.settings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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

data class PolicySection(
    val icon: ImageVector,
    val title: String,
    val content: String,
    val bulletPoints: List<String> = emptyList()
)

/*
 * Description: Premium Privacy Policy screen with professional legal content display
 * Params: onBack - callback to navigate back
 * Returns: Scrollable privacy policy with animated sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    val policySections = remember {
        listOf(
            PolicySection(
                icon = Icons.Default.Info,
                title = "Introduction",
                content = "Omni Pad (\"we\", \"our\", or \"us\") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our mobile application."
            ),
            PolicySection(
                icon = Icons.Default.DataUsage,
                title = "Information We Collect",
                content = "We may collect information about you in various ways:",
                bulletPoints = listOf(
                    "Device Information: Device type, operating system, unique device identifiers",
                    "Usage Data: App features used, time spent, interaction patterns",
                    "Video Preferences: Watch history, downloads, quality preferences",
                    "Support Requests: Information you provide when contacting support"
                )
            ),
            PolicySection(
                icon = Icons.Default.Storage,
                title = "How We Use Your Information",
                content = "We use the information we collect to:",
                bulletPoints = listOf(
                    "Provide, operate, and maintain the application",
                    "Improve and personalize your experience",
                    "Understand and analyze usage patterns",
                    "Develop new features and functionality",
                    "Communicate with you for support and updates",
                    "Ensure the security and integrity of our services"
                )
            ),
            PolicySection(
                icon = Icons.Default.CloudDownload,
                title = "Offline Data Storage",
                content = "When you enable offline mode, videos and associated metadata are stored locally on your device. This data remains on your device and is not transmitted to our servers. You can delete offline content at any time through the app settings."
            ),
            PolicySection(
                icon = Icons.Default.Share,
                title = "Information Sharing",
                content = "We do not sell, trade, or rent your personal information to third parties. We may share information only in the following circumstances:",
                bulletPoints = listOf(
                    "With your consent",
                    "To comply with legal obligations",
                    "To protect our rights and safety",
                    "With service providers who assist our operations"
                )
            ),
            PolicySection(
                icon = Icons.Default.Security,
                title = "Data Security",
                content = "We implement appropriate technical and organizational security measures to protect your information against unauthorized access, alteration, disclosure, or destruction. However, no method of transmission over the Internet or electronic storage is 100% secure."
            ),
            PolicySection(
                icon = Icons.Default.ChildCare,
                title = "Children's Privacy",
                content = "Our application is not intended for children under 13 years of age. We do not knowingly collect personal information from children under 13. If you believe we have collected information from a child under 13, please contact us immediately."
            ),
            PolicySection(
                icon = Icons.Default.Update,
                title = "Policy Updates",
                content = "We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page and updating the \"Last Updated\" date. You are advised to review this Privacy Policy periodically for any changes."
            ),
            PolicySection(
                icon = Icons.Default.Email,
                title = "Contact Us",
                content = "If you have any questions about this Privacy Policy or our data practices, please contact us through the Contact Support section in the app settings, or email us at privacy@hp.com."
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
                                    text = "Privacy Policy",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Last updated: March 2026",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
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
                                    Icons.Default.Shield,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PremiumBlue
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = "Your Privacy Matters",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Text(
                                    text = "We are committed to protecting your personal information and being transparent about what we collect.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Policy sections
            itemsIndexed(policySections) { index, section ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = (index + 1) * 100)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                ) {
                    PolicySectionCard(section)
                }
            }
            
            // Footer
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = policySections.size * 100))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = PremiumBlue,
                                modifier = Modifier.size(32.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Omni Pad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "© 2026 HP Inc. All rights reserved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun PolicySectionCard(section: PolicySection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            PremiumBlue.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        tint = PremiumBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            
            if (section.bulletPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                section.bulletPoints.forEach { point ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .background(PremiumBlue, CircleShape)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
