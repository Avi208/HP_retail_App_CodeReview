package com.hp.hp_omnipad.ui.home.settings.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.data.repository.FirestoreRepository
import com.hp.hp_omnipad.ui.theme.LightGreyButton
import com.hp.hp_omnipad.ui.theme.PrimaryBlue
import com.hp.hp_omnipad.ui.theme.SecondaryText
import com.hp.hp_omnipad.utils.FileDownloader
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SecurityRow() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var clearDataDialog by remember { mutableStateOf(false) }
    var clearCacheDialog by remember { mutableStateOf(false) }
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            // Security Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LightGreyButton, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.security_settings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.app_data_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Expandable security options
            if (expanded) {
                Spacer(modifier = Modifier.height(20.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Clear Cache Option
                SecurityOption(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.clear_cache),
                    subtitle = stringResource(R.string.clear_cache_subtitle),
                    onClick = { clearCacheDialog = true }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Clear App Data Option
                SecurityOption(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.clear_app_data),
                    subtitle = stringResource(R.string.clear_app_data_subtitle),
                    onClick = { clearDataDialog = true },
                    isDestructive = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Privacy Policy
                SecurityOption(
                    icon = Icons.Default.Policy,
                    title = stringResource(R.string.privacy_policy),
                    subtitle = "View our privacy terms", // Consider adding to strings.xml if needed
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hp.com/privacy"))
                        context.startActivity(intent)
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Terms of Service
                SecurityOption(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.terms_of_service),
                    subtitle = stringResource(R.string.terms_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hp.com/terms"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
    
    // Clear Cache Confirmation Dialog
    if (clearCacheDialog) {
        AlertDialog(
            onDismissRequest = { clearCacheDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { 
                Text(
                    stringResource(R.string.clear_cache),
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(
                    "This will clear cached images and video data. " +
                    "Your downloaded videos will not be affected."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Clear Coil image cache
                        Coil.imageLoader(context).memoryCache?.clear()
                        Coil.imageLoader(context).diskCache?.clear()
                        
                        // Clear app cache directory
                        context.cacheDir.deleteRecursively()
                        
                        // Clear repository cache
                        FirestoreRepository.clearCache()
                        
                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                        clearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear_cache))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Clear Data Confirmation Dialog
    if (clearDataDialog) {
        AlertDialog(
            onDismissRequest = { clearDataDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { 
                Text(
                    stringResource(R.string.clear_data_title),
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(stringResource(R.string.clear_data_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Clear Coil image cache
                        Coil.imageLoader(context).memoryCache?.clear()
                        Coil.imageLoader(context).diskCache?.clear()
                        
                        // Clear app cache
                        context.cacheDir.deleteRecursively()
                        
                        // Clear app files (internal storage)
                        context.filesDir.deleteRecursively()
                        
                        // Clear repository cache
                        FirestoreRepository.clearCache()
                        
                        // Clear downloaded videos from Movies/OmniPad
                        val downloadsDir = File(
                            android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_MOVIES
                            ),
                            "OmniPad"
                        )
                        if (downloadsDir.exists()) {
                            downloadsDir.deleteRecursively()
                        }
                        
                        // Clear SharedPreferences
                        context.getSharedPreferences("hp_omnipad_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply()
                        
                        Toast.makeText(context, "App data cleared successfully", Toast.LENGTH_SHORT).show()
                        clearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.clear_data))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDataDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SecurityOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}