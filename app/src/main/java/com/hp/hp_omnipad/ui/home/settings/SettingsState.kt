package com.hp.hp_omnipad.ui.home.settings

data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val selectedLanguage: String = "English",
    val stayAwakeEnabled: Boolean = false,
    val offloadAppEnabled: Boolean = true,
    val selectedVideoQuality: String = "Auto"
)
