package com.hp.hp_omnipad.ui.home.settings.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.hp.hp_omnipad.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private val PremiumBlue = Color(0xFF0096D6)
private val PremiumGradientStart = Color(0xFF0096D6)
private val PremiumGradientEnd = Color(0xFF00D4AA)
private val SuccessGreen = Color(0xFF4CAF50)

enum class TicketCategory(val labelRes: Int, val icon: ImageVector) {
    GENERAL(R.string.general_inquiry, Icons.Default.QuestionAnswer),
    BUG(R.string.bug_report, Icons.Default.BugReport),
    FEATURE(R.string.feature_request, Icons.Default.Lightbulb),
    ACCOUNT(R.string.account_issue, Icons.Default.Person),
    PLAYBACK(R.string.video_playback, Icons.Default.PlayCircle),
    OTHER(R.string.other, Icons.Default.MoreHoriz)
}

enum class TicketPriority(val labelRes: Int, val color: Color) {
    LOW(R.string.low, Color(0xFF4CAF50)),
    MEDIUM(R.string.medium, Color(0xFFFF9800)),
    HIGH(R.string.high, Color(0xFFF44336))
}

/*
 * Description: Premium Contact Support screen with form submission to Firebase
 * Params: onBack - callback to navigate back
 * Returns: Form UI for submitting support tickets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var isVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    // Form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TicketCategory.GENERAL) }
    var selectedPriority by remember { mutableStateOf(TicketPriority.MEDIUM) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    // Validation
    val isFormValid = name.isNotBlank() && 
                      email.isNotBlank() && 
                      email.contains("@") &&
                      subject.isNotBlank() && 
                      message.isNotBlank()
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    // Success animation scale
    val successScale by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success"
    )
    
    fun submitTicket() {
        if (!isFormValid) return
        
        isSubmitting = true
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val ticketData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "subject" to subject,
                    "message" to message,
                    "category" to selectedCategory.name,
                    "priority" to selectedPriority.name,
                    "status" to "OPEN",
                    "createdAt" to System.currentTimeMillis(),
                    "createdAtFormatted" to SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
                )
                
                firestore.collection("support_tickets")
                    .add(ticketData)
                    .await()
                
                isSubmitting = false
                showSuccess = true
                
                // Reset form after delay
                delay(2500)
                onBack()
                
            } catch (e: Exception) {
                isSubmitting = false
                Toast.makeText(context, "Failed to submit: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    if (showSuccess) {
        // Success overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(successScale)
                    .padding(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.7f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.ticket_submitted),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.ticket_submitted_thanks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
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
                                        text = stringResource(R.string.contact_support),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.contact_support_subtitle),
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
                                        Icons.Default.SupportAgent,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header info card
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
                        colors = CardDefaults.cardColors(containerColor = PremiumBlue)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Headset,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = stringResource(R.string.support_available_24_7),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Text(
                                    text = stringResource(R.string.support_form_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                
                // Form card
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = PremiumBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.submit_ticket),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            // Name field
                            PremiumTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(R.string.full_name),
                                leadingIcon = Icons.Default.Person,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            // Email field
                            PremiumTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = stringResource(R.string.email_address),
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                isError = email.isNotBlank() && !email.contains("@")
                            )
                            
                            // Category dropdown
                            ExposedDropdownMenuBox(
                                expanded = showCategoryDropdown,
                                onExpandedChange = { showCategoryDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = stringResource(selectedCategory.labelRes),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.category)) },
                                    leadingIcon = {
                                        Icon(selectedCategory.icon, null, tint = PremiumBlue)
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PremiumBlue,
                                        focusedLabelColor = PremiumBlue
                                    )
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = showCategoryDropdown,
                                    onDismissRequest = { showCategoryDropdown = false }
                                ) {
                                    TicketCategory.entries.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        category.icon,
                                                        null,
                                                        tint = PremiumBlue,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(stringResource(category.labelRes))
                                                }
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Priority selection
                            Column {
                                Text(
                                    text = stringResource(R.string.priority),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TicketPriority.entries.forEach { priority ->
                                        PriorityChip(
                                            priority = priority,
                                            isSelected = selectedPriority == priority,
                                            onClick = { selectedPriority = priority },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            // Subject field
                            PremiumTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = stringResource(R.string.subject),
                                leadingIcon = Icons.Default.Subject,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            // Message field
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text(stringResource(R.string.describe_issue)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Message, null, tint = PremiumBlue)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PremiumBlue,
                                    focusedLabelColor = PremiumBlue
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Submit button
                            Button(
                                onClick = { submitTicket() },
                                enabled = isFormValid && !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PremiumBlue
                                )
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.submit_ticket),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Contact info footer
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = 200))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.other_ways_to_reach_us),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Email,
                                    null,
                                    tint = PremiumBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "support@hp.com",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    null,
                                    tint = PremiumBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1-800-474-6836",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(leadingIcon, null, tint = if (isError) MaterialTheme.colorScheme.error else PremiumBlue)
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PremiumBlue,
            focusedLabelColor = PremiumBlue
        )
    )
}

@Composable
private fun PriorityChip(
    priority: TicketPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) priority.color else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = stringResource(priority.labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}