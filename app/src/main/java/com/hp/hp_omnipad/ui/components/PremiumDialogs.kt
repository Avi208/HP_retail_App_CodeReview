package com.hp.hp_omnipad.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/*
 * Description: Premium animated low battery warning dialog
 * Params: batteryLevel - current battery percentage, onDismiss - close callback
 * Returns: Animated dialog with battery indicator and warning message
 */
@Composable
fun PremiumBatteryWarningDialog(
    batteryLevel: Int,
    onDismiss: () -> Unit
) {
    val batteryColor = when {
        batteryLevel <= 5 -> Color(0xFFE53935)
        batteryLevel <= 10 -> Color(0xFFFF5722)
        batteryLevel <= 15 -> Color(0xFFFF9800)
        batteryLevel <= 20 -> Color(0xFFFFC107)
        else -> Color(0xFFFFEB3B)
    }
    
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val glowAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val progressAnim by animateFloatAsState(
        targetValue = batteryLevel / 100f,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "progress"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated battery indicator
                Box(
                    modifier = Modifier
                        .size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .background(
                                batteryColor.copy(alpha = glowAlpha * 0.3f),
                                CircleShape
                            )
                    )
                    
                    // Circular progress background
                    Canvas(modifier = Modifier.size(120.dp)) {
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                    }
                    
                    // Animated progress arc
                    Canvas(modifier = Modifier.size(120.dp)) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    batteryColor.copy(alpha = 0.6f),
                                    batteryColor,
                                    batteryColor.copy(alpha = 0.8f)
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progressAnim,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                    }
                    
                    // Center content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (batteryLevel <= 10) Icons.Default.BatteryAlert else Icons.Default.Battery3Bar,
                            contentDescription = null,
                            tint = batteryColor,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = batteryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Warning title with gradient
                Text(
                    text = when {
                        batteryLevel <= 5 -> "Critical Battery!"
                        batteryLevel <= 10 -> "Very Low Battery"
                        batteryLevel <= 15 -> "Low Battery"
                        else -> "Battery Warning"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = batteryColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message
                Text(
                    text = "Please connect your charger to continue using the app without interruption.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Charging suggestion
                Surface(
                    color = batteryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = batteryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Plug in charger now",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = batteryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = batteryColor
                    )
                ) {
                    Text(
                        "Got it",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/*
 * Description: Premium confirmation dialog with animations
 * Params: icon - display icon, title - dialog title, message - content, callbacks
 * Returns: Animated confirmation dialog
 */
@Composable
fun PremiumConfirmationDialog(
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .scale(scaleAnim)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            iconColor.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(dismissText, fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iconColor
                        )
                    ) {
                        Text(confirmText, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/*
 * Description: Premium loading dialog with animated indicator
 * Params: message - loading message to display
 * Returns: Animated loading dialog
 */
@Composable
fun PremiumLoadingDialog(
    message: String = "Loading..."
) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
