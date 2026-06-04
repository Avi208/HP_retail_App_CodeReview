package com.hp.hp_omnipad.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hp.hp_omnipad.ui.theme.AppBlue

/*
 * Description: Dialog for editing or creating a video
 * Params: video - existing VideoData to edit (null for new), onDismiss - close callback, onSave - save callback
 * Returns: Dialog UI with form fields for video properties
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditDialog(
    video: VideoData?,
    onDismiss: () -> Unit,
    onSave: (VideoData) -> Unit
) {
    var title by remember { mutableStateOf(video?.title ?: "") }
    var description by remember { mutableStateOf(video?.description ?: "") }
    var videoUrl by remember { mutableStateOf(video?.videoUrl ?: "") }
    var thumbnailUrl by remember { mutableStateOf(video?.thumbnailUrl ?: "") }
    var durationSec by remember { mutableStateOf(video?.durationSec?.toString() ?: "0") }
    var published by remember { mutableStateOf(video?.published ?: true) }
    var categoryIds by remember { mutableStateOf(video?.categoryIds?.joinToString(", ") ?: "") }
    var tags by remember { mutableStateOf(video?.tags?.joinToString(", ") ?: "") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (video != null) "Edit Video" else "Add New Video",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Video URL
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video URL *") },
                    leadingIcon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Thumbnail URL
                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text("Thumbnail URL") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration
                OutlinedTextField(
                    value = durationSec,
                    onValueChange = { durationSec = it.filter { c -> c.isDigit() } },
                    label = { Text("Duration (seconds)") },
                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Categories
                OutlinedTextField(
                    value = categoryIds,
                    onValueChange = { categoryIds = it },
                    label = { Text("Categories (comma separated)") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    placeholder = { Text("e.g., demos, training") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                    placeholder = { Text("e.g., laptop, review") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Published toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (published) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (published) AppBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Published",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (published) "Video is visible to users" else "Video is hidden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = published,
                        onCheckedChange = { published = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppBlue)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            onSave(
                                VideoData(
                                    id = video?.id ?: "",
                                    title = title,
                                    description = description,
                                    videoUrl = videoUrl,
                                    thumbnailUrl = thumbnailUrl,
                                    durationSec = durationSec.toLongOrNull() ?: 0,
                                    viewCount = video?.viewCount ?: 0,
                                    published = published,
                                    categoryIds = categoryIds.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    language = "en",
                                    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && videoUrl.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppBlue)
                    ) {
                        Text(if (video != null) "Update" else "Create")
                    }
                }
            }
        }
    }
}

/*
 * Description: Dialog for editing or creating a hero video
 * Params: hero - existing HeroData to edit (null for new), onDismiss - close callback, onSave - save callback
 * Returns: Dialog UI with form fields for hero properties
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroEditDialog(
    hero: HeroData?,
    onDismiss: () -> Unit,
    onSave: (HeroData) -> Unit
) {
    var title by remember { mutableStateOf(hero?.title ?: "") }
    var videoUrl by remember { mutableStateOf(hero?.videoUrl ?: "") }
    var thumbnailUrl by remember { mutableStateOf(hero?.thumbnailUrl ?: "") }
    var active by remember { mutableStateOf(hero?.active ?: true) }
    var order by remember { mutableStateOf(hero?.order?.toString() ?: "0") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hero != null) "Edit Hero Video" else "Add Hero Video",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Video URL
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video URL *") },
                    leadingIcon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Thumbnail URL
                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text("Thumbnail URL") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it.filter { c -> c.isDigit() } },
                    label = { Text("Display Order") },
                    leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Active toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (active) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (active) AppBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (active) "Hero is displayed on home" else "Hero is hidden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppBlue)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            onSave(
                                HeroData(
                                    id = hero?.id ?: "",
                                    title = title,
                                    videoUrl = videoUrl,
                                    thumbnailUrl = thumbnailUrl,
                                    active = active,
                                    order = order.toIntOrNull() ?: 0
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && videoUrl.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppBlue)
                    ) {
                        Text(if (hero != null) "Update" else "Create")
                    }
                }
            }
        }
    }
}
