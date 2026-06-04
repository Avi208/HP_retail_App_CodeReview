package com.hp.hp_omnipad.ui.home.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hp.hp_omnipad.ui.theme.LightGreyButton
import com.hp.hp_omnipad.ui.theme.PrimaryBlue
import com.hp.hp_omnipad.ui.theme.SecondaryText

@Composable
fun DeveloperSettingsCard(
    offloadEnabled: Boolean,
    onToggleOffload: (Boolean) -> Unit
) {
    var showDisableWarning by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                DevSettingIcon(Icons.Default.CloudDownload)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offload App",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (offloadEnabled) "Downloads videos for offline use" 
                               else "Streams videos from internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                Switch(
                    checked = offloadEnabled,
                    onCheckedChange = { newValue ->
                        if (!newValue && offloadEnabled) {
                            showDisableWarning = true
                        } else {
                            onToggleOffload(true)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PrimaryBlue,
                        uncheckedTrackColor = LightGreyButton,
                        checkedThumbColor = Color.White
                    )
                )
            }
        }
    }
    
    if (showDisableWarning) {
        AlertDialog(
            onDismissRequest = { showDisableWarning = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { 
                Text(
                    "Disable Offline Mode?",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    Text(
                        "Disabling offload mode will:",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Delete all downloaded videos")
                    Text("• Free up storage space")
                    Text("• All videos will stream from internet")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This action cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleOffload(false)
                        showDisableWarning = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disable & Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DevSettingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(LightGreyButton, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue
        )
    }
}
