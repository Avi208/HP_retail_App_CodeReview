package com.hp.hp_omnipad.ui.home.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hp.hp_omnipad.R
import com.hp.hp_omnipad.ui.home.settings.SettingsState
import com.hp.hp_omnipad.ui.theme.CardBackground
import com.hp.hp_omnipad.ui.theme.LightGreyButton
import com.hp.hp_omnipad.ui.theme.PrimaryBlue
import com.hp.hp_omnipad.ui.theme.SecondaryText

@Composable
fun PreferencesCard(
    state: SettingsState,
    isDark: Boolean,
    onToggleNotifications: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onToggleStayAwake: () -> Unit = {}
){

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Push Notifications
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                SettingIcon(Icons.Default.Notifications)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.push_notifications),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.notification_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { onToggleNotifications() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PrimaryBlue,
                        uncheckedTrackColor = LightGreyButton,
                        checkedThumbColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Language
            SettingRow(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language)
            ) {
                LanguageDropdown(
                    selected = state.selectedLanguage,
                    onLanguageChange = onLanguageChange
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Theme
            SettingRow(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.theme)
            ) {
                ThemeSelector(
                    isDark = isDark,
                    onThemeChange = onThemeChange
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Stay Awake
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                SettingIcon(Icons.Default.Visibility)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stay_awake),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.stay_awake_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                Switch(
                    checked = state.stayAwakeEnabled,
                    onCheckedChange = { onToggleStayAwake() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PrimaryBlue,
                        uncheckedTrackColor = LightGreyButton,
                        checkedThumbColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailingContent: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingIcon(icon)

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        trailingContent()
    }
}

@Composable
private fun SettingIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

private val languages = listOf(
    LanguageOption("en", "English", "English", "🇺🇸"),
    LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳")
)

@Composable
private fun LanguageDropdown(
    selected: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLang = languages.find { it.name == selected } ?: languages.first()

    Box {
        Card(
            modifier = Modifier
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = LightGreyButton
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLang.flag,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedLang.nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = SecondaryText
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = lang.flag,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = lang.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (selected == lang.name) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageChange(lang.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = LightGreyButton,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {

        val lightSelected = !isDark
        val darkSelected = isDark

        Text(
            text = "☀ " + stringResource(R.string.light_theme),
            modifier = Modifier
                .background(
                    if (lightSelected) CardBackground else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onThemeChange(false) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (lightSelected) PrimaryBlue else SecondaryText,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "🌙 " + stringResource(R.string.dark_theme),
            modifier = Modifier
                .background(
                    if (darkSelected) CardBackground else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onThemeChange(true) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (darkSelected) PrimaryBlue else SecondaryText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}