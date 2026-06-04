package com.hp.hp_omnipad.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hp.hp_omnipad.utils.FileDownloader

data class ResourceItem(
    val title: String,
    val url: String
)

@Composable
fun ResourcesDropdown(resources: List<ResourceItem>) {

    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    if (resources.isEmpty()) return

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Resources",
                style = MaterialTheme.typography.titleMedium
            )

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            resources.forEach { resource ->

                DropdownMenuItem(
                    text = { Text(resource.title) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null
                        )
                    },
                    onClick = {

                        expanded = false

                        val fileName =
                            if (resource.title.endsWith(".pdf"))
                                resource.title
                            else
                                "${resource.title}.pdf"

                        FileDownloader.downloadResourceFile(
                            context = context,
                            url = resource.url,
                            fileName = fileName
                        )
                    }
                )
            }
        }
    }
}